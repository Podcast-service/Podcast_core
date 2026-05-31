package podcastService.author.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import podcastService.author.dto.*;
import podcastService.author.entity.AuthorEntity;
import podcastService.author.mapper.AuthorMapper;
import podcastService.author.repository.AuthorRepository;
import podcastService.author.specifications.AuthorSpecifications;
import podcastService.author.util.AuthorPageableFactory;
import podcastService.common.dto.PageMeta;
import podcastService.common.dto.PageResponse;
import podcastService.common.exception.BadRequestException;
import podcastService.common.exception.ConflictException;
import podcastService.common.exception.NotFoundException;
import podcastService.subscription.repository.SubscriptionRepository;
import podcastService.user.entity.UserProfileEntity;
import podcastService.user.repository.UserProfileRepository;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorProfileService {

    private final AuthorRepository authorRepository;
    private final UserProfileRepository userProfileRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final AuthorMapper authorMapper;

    @Transactional
    public AuthorProfileResponse create(UUID currentUserId, CreateAuthorProfileRequest request) {
        UserProfileEntity userProfile = requireUserProfile(currentUserId);

        if (authorRepository.existsByUserProfileId(userProfile.getId())) {
            throw new ConflictException("Author profile already exists");
        }

        AuthorEntity author = new AuthorEntity();
        author.setUserProfile(userProfile);
        author.setAuthorName(normalizeAuthorName(request.authorName()));
        author.setDescription(normalizeNullableText(request.description()));

        try {
            AuthorEntity saved = authorRepository.saveAndFlush(author);
            log.info(
                    "Author profile created: authorId={}, userId={}, userProfileId={}",
                    saved.getId(),
                    currentUserId,
                    userProfile.getId()
            );
            return authorMapper.toProfileResponse(saved, false);
        } catch (DataIntegrityViolationException exception) {
            log.warn(
                    "Author profile creation failed due to integrity violation, userId={}, userProfileId={}",
                    currentUserId,
                    userProfile.getId()
            );
            throw new ConflictException("Author profile already exists");
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<AuthorCard> getAuthors(AuthorFilter filter, UUID currentUserId) {
        Specification<AuthorEntity> specification = Specification
                .where(AuthorSpecifications.fetchUserProfile())
                .and(AuthorSpecifications.searchByText(filter.q()));

        Page<AuthorEntity> authorPage = authorRepository.findAll(
                specification,
                AuthorPageableFactory.create(
                        filter.normalizedPage(),
                        filter.normalizedSize(),
                        filter.normalizedSort()
                )
        );

        Set<UUID> subscribedAuthorIds = resolveSubscribedAuthorIds(authorPage, currentUserId);
        Page<AuthorCard> page = authorPage.map(entity -> authorMapper.toCard(
                entity,
                subscribedAuthorIds == null ? null : subscribedAuthorIds.contains(entity.getId())
        ));

        log.debug(
                "Authors listed: currentUserId={}, page={}, size={}, totalElements={}",
                currentUserId,
                filter.normalizedPage(),
                filter.normalizedSize(),
                page.getTotalElements()
        );

        return new PageResponse<>(
                page.getContent(),
                new PageMeta(
                        filter.normalizedPage(),
                        page.getSize(),
                        page.getTotalElements(),
                        page.getTotalPages()
                )
        );

    }

    private Set<UUID> resolveSubscribedAuthorIds(Page<AuthorEntity> authorPage, UUID currentUserId) {
        if (currentUserId == null) {
            return null;
        }

        return userProfileRepository.findByUserId(currentUserId)
                .map(userProfile -> {
                    Set<UUID> authorIds = authorPage.getContent().stream()
                            .map(AuthorEntity::getId)
                            .collect(Collectors.toSet());
                    if (authorIds.isEmpty()) {
                        return Set.<UUID>of();
                    }
                    return subscriptionRepository.findSubscribedAuthorIds(userProfile.getId(), authorIds);
                })
                .orElse(Set.of());
    }

    @Transactional
    public AuthorProfileCreationResult createOrGet(UUID currentUserId, CreateAuthorProfileRequest request) {
        UserProfileEntity userProfile = requireUserProfile(currentUserId);

        return authorRepository.findByUserProfileId(userProfile.getId())
                .map(existing -> {
                    log.info(
                            "Author profile creation skipped because profile already exists, authorId={}, userId={}, userProfileId={}",
                            existing.getId(),
                            currentUserId,
                            userProfile.getId()
                    );
                    return new AuthorProfileCreationResult(authorMapper.toProfileResponse(existing, false), false);
                })
                .orElseGet(() -> createNewAuthorProfile(currentUserId, request, userProfile));
    }

    @Transactional(readOnly = true)
    public void validateCreateRequestForBecomeAuthor(UUID currentUserId, CreateAuthorProfileRequest request) {
        UserProfileEntity userProfile = requireUserProfile(currentUserId);
        if (authorRepository.existsByUserProfileId(userProfile.getId())) {
            return;
        }

        normalizeAuthorName(request.authorName());
        normalizeNullableText(request.description());
    }

    @Transactional(readOnly = true)
    public AuthorProfileResponse getMine(UUID currentUserId) {
        AuthorEntity author = authorRepository.findByUserProfileUserId(currentUserId)
                .orElseThrow(() -> new NotFoundException("Author profile not found"));

        return authorMapper.toProfileResponse(author, false);
    }

    @Transactional
    public AuthorProfileResponse updateMine(UUID currentUserId, UpdateAuthorProfileRequest request) {
        validateUpdateRequest(request);

        AuthorEntity author = authorRepository.findByUserProfileUserIdForUpdate(currentUserId)
                .orElseThrow(() -> new NotFoundException("Author profile not found"));

        boolean changed = false;

        if (request.isAuthorNameSet()) {
            String authorName = normalizeAuthorName(request.getAuthorName());
            if (!authorName.equals(author.getAuthorName())) {
                author.setAuthorName(authorName);
                changed = true;
            }
        }

        if (request.isDescriptionSet()) {
            String description = normalizeNullableText(request.getDescription());
            if (!equalsNullable(description, author.getDescription())) {
                author.setDescription(description);
                changed = true;
            }
        }

        if (!changed) {
            log.info("Author profile update skipped because no effective changes detected, userId={}", currentUserId);
            return authorMapper.toProfileResponse(author, false);
        }

        AuthorEntity saved = authorRepository.saveAndFlush(author);
        log.info(
                "Author profile updated: authorId={}, userId={}, authorNameChanged={}, descriptionChanged={}",
                saved.getId(),
                currentUserId,
                request.isAuthorNameSet(),
                request.isDescriptionSet()
        );

        return authorMapper.toProfileResponse(saved, false);
    }

    @Transactional(readOnly = true)
    public AuthorProfileResponse getPublic(UUID authorId, UUID currentUserId) {
        AuthorEntity author = authorRepository.findDetailedById(authorId)
                .orElseThrow(() -> new NotFoundException("Author not found"));

        Boolean isSubscribed = resolveSubscriptionStatus(authorId, currentUserId, author.getUserProfile().getId());
        return authorMapper.toProfileResponse(author, isSubscribed);
    }

    private UserProfileEntity requireUserProfile(UUID currentUserId) {
        return userProfileRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new NotFoundException("User profile not found: " + currentUserId));
    }

    private AuthorProfileCreationResult createNewAuthorProfile(
            UUID currentUserId,
            CreateAuthorProfileRequest request,
            UserProfileEntity userProfile
    ) {
        AuthorEntity author = new AuthorEntity();
        author.setUserProfile(userProfile);
        author.setAuthorName(normalizeAuthorName(request.authorName()));
        author.setDescription(normalizeNullableText(request.description()));

        try {
            AuthorEntity saved = authorRepository.saveAndFlush(author);
            log.info(
                    "Author profile created: authorId={}, userId={}, userProfileId={}",
                    saved.getId(),
                    currentUserId,
                    userProfile.getId()
            );
            return new AuthorProfileCreationResult(authorMapper.toProfileResponse(saved, false), true);
        } catch (DataIntegrityViolationException exception) {
            log.warn(
                    "Author profile creation raced with another request, userId={}, userProfileId={}",
                    currentUserId,
                    userProfile.getId()
            );
            AuthorEntity existing = authorRepository.findByUserProfileId(userProfile.getId())
                    .orElseThrow(() -> exception);
            return new AuthorProfileCreationResult(authorMapper.toProfileResponse(existing, false), false);
        }
    }

    private Boolean resolveSubscriptionStatus(UUID authorId, UUID currentUserId, UUID authorOwnerProfileId) {
        if (currentUserId == null) {
            return null;
        }

        return userProfileRepository.findByUserId(currentUserId)
                .map(currentUser -> {
                    if (currentUser.getId().equals(authorOwnerProfileId)) {
                        return false;
                    }
                    return subscriptionRepository.existsByIdSubscriberProfileIdAndIdAuthorId(
                            currentUser.getId(),
                            authorId
                    );
                })
                .orElse(false);
    }

    private void validateUpdateRequest(UpdateAuthorProfileRequest request) {
        if (!request.isAuthorNameSet() && !request.isDescriptionSet()) {
            throw new BadRequestException("At least one field must be provided for update");
        }
    }

    private String normalizeAuthorName(String value) {
        String normalized = normalizeRequiredText(value, "authorName");
        if (normalized.length() < 2 || normalized.length() > 100) {
            throw validationError("authorName", "authorName must be between 2 and 100 characters");
        }
        return normalized;
    }

    private String normalizeRequiredText(String value, String fieldName) {
        if (value == null) {
            throw validationError(fieldName, fieldName + " must not be null");
        }

        String normalized = value.trim();
        if (normalized.isBlank()) {
            throw validationError(fieldName, fieldName + " must not be blank");
        }

        return normalized;
    }

    private String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private boolean equalsNullable(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }

    private BadRequestException validationError(String fieldName, String message) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put(fieldName, message);
        return new BadRequestException("Request validation failed", Map.of("fields", fields));
    }
}
