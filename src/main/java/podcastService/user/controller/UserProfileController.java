package podcastService.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import podcastService.user.dto.UpdateUserProfileRequest;
import podcastService.user.dto.UpdateUserSettingsRequest;
import podcastService.user.dto.UserProfilePrivateResponse;
import podcastService.user.dto.UserSettingsResponse;
import podcastService.user.service.UserProfileService;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/users/me")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class UserProfileController {
    private final UserProfileService userProfileService;

    @GetMapping("/profile")
    public UserProfilePrivateResponse getUserProfile(@RequestHeader("X-User-Id") UUID currentUserId) {
        log.info("GET /users/me/profile, userId={}", currentUserId);
        return userProfileService.findUserByUserId(currentUserId);
    }

    @PutMapping("/profile")
    public UserProfilePrivateResponse updateUserProfile(
            @RequestHeader("X-User-Id") UUID currentUserId,
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        log.info("PUT /users/me/profile, userId={}, usernameChanged={}, avatarChanged={}",
                currentUserId,
                request.username() != null,
                request.avatarUrl() != null
        );
        return userProfileService.updateUserProfile(currentUserId, request);
    }

    @GetMapping("/settings")
    public UserSettingsResponse getUserSettings(@RequestHeader("X-User-Id") UUID currentUserId) {
        log.info("GET /users/me/settings, userId={}", currentUserId);
        return userProfileService.getUserSettings(currentUserId);
    }

    @PutMapping("/settings")
    public UserSettingsResponse updateUserSettings(
            @RequestHeader("X-User-Id") UUID currentUserId,
            @Valid @RequestBody UpdateUserSettingsRequest request
    ) {
        log.info("PUT /users/me/settings, userId={}, theme={}, language={}",
                currentUserId, request.theme(), request.language());
        return userProfileService.updateUserSettings(currentUserId, request);
    }
}
