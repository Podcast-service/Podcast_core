package podcastService.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import podcastService.common.exception.BadRequestException;
import podcastService.common.exception.NotFoundException;
import podcastService.infrastructure.messaging.error.InvalidKafkaMessageException;
import podcastService.user.dto.CreateUserRequest;
import podcastService.user.dto.UpdateUserProfileRequest;
import podcastService.user.dto.UpdateUserSettingsRequest;
import podcastService.user.dto.UserProfilePrivateResponse;
import podcastService.user.dto.UserSettingsResponse;
import podcastService.user.entity.UserProfileEntity;
import podcastService.user.mapper.UserProfileMapper;
import podcastService.user.repository.UserProfileRepository;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository userRepository;
    private final UserProfileMapper userMapper;

    @Transactional(readOnly = true)
    public UserProfilePrivateResponse findUserByUserId(UUID userId) {
        UserProfileEntity userEntity = userRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("User profile not found: " + userId));

        return userMapper.toPrivateResponse(userEntity);
    }

    @Transactional(readOnly = true)
    public UserSettingsResponse getUserSettings(UUID userId) {
        UserProfileEntity userEntity = userRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("User profile not found: " + userId));

        return new UserSettingsResponse(
                userEntity.getTheme(),
                userEntity.getLanguage()
        );
    }

    @Transactional
    public UserProfilePrivateResponse updateUserProfile(UUID userId, UpdateUserProfileRequest request) {
        UserProfileEntity user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("User profile not found: " + userId));

        boolean changed = false;

        if (request.username() != null) {
            String normalizedUsername = request.username().trim();

            if (normalizedUsername.isBlank()) {
                throw validationError("must not be blank");
            }

            if (!normalizedUsername.equals(user.getUsername())) {
                user.setUsername(normalizedUsername);
                changed = true;
            }
        }

        if (request.avatarUrl() != null) {
            String normalizedAvatarUrl = request.avatarUrl().trim();
            String newAvatarUrl = normalizedAvatarUrl.isBlank() ? null : normalizedAvatarUrl;

            if (!equalsNullable(user.getAvatarUrl(), newAvatarUrl)) {
                user.setAvatarUrl(newAvatarUrl);
                changed = true;
            }
        }

        if (!changed) {
            log.info("User profile update skipped because no effective changes detected, userId={}", userId);
            return userMapper.toPrivateResponse(user);
        }

        try {
            UserProfileEntity updatedUser = userRepository.save(user);
            log.info("User profile updated successfully, userId={}", userId);
            return userMapper.toPrivateResponse(updatedUser);
        } catch (DataIntegrityViolationException exception) {
            log.warn("User profile update failed due to integrity violation, userId={}", userId);
            throw validationError("username already exists");
        }
    }

    @Transactional
    public UserSettingsResponse updateUserSettings(UUID userId, UpdateUserSettingsRequest request) {
        UserProfileEntity user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("User profile not found: " + userId));

        boolean changed = false;

        if (request.theme() != null && request.theme() != user.getTheme()) {
            user.setTheme(request.theme());
            changed = true;
        }

        if (request.language() != null && request.language() != user.getLanguage()) {
            user.setLanguage(request.language());
            changed = true;
        }

        if (!changed) {
            log.info("User settings update skipped because no effective changes detected, userId={}", userId);
            return new UserSettingsResponse(user.getTheme(), user.getLanguage());
        }

        UserProfileEntity updatedUser = userRepository.save(user);

        log.info(
                "User settings updated successfully, userId={}, theme={}, language={}",
                userId,
                updatedUser.getTheme(),
                updatedUser.getLanguage()
        );

        return new UserSettingsResponse(
                updatedUser.getTheme(),
                updatedUser.getLanguage()
        );
    }

    @Transactional
    public void createNewUser(CreateUserRequest request) {
        if (request == null) {
            throw new InvalidKafkaMessageException("Received empty Kafka request");
        }

        if (request.userId() == null) {
            throw new InvalidKafkaMessageException("Received null userId in Kafka");
        }

        if (request.username() == null) {
            throw new InvalidKafkaMessageException("Received null username in Kafka");
        }

        String normalizedUsername = request.username().trim();

        if (normalizedUsername.isBlank()) {
            throw new InvalidKafkaMessageException("Received blank username in Kafka");
        }

        try {
            userRepository.insertUserProfile(request.userId(), normalizedUsername);
            log.info("User profile created successfully from Kafka event, userId={}", request.userId());
        } catch (DataIntegrityViolationException exception) {
            log.warn("User profile creation failed due to integrity violation, userId={}", request.userId());
            throw new InvalidKafkaMessageException("Cannot create user profile due to duplicate or invalid data");
        }
    }

    private BadRequestException validationError(String message) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("username", message);

        return new BadRequestException(
                "Request validation failed",
                Map.of("fields", fields)
        );
    }

    private boolean equalsNullable(String left, String right) {
        return Objects.equals(left, right);
    }
}
