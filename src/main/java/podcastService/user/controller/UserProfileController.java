package podcastService.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import podcastService.infrastructure.security.AuthenticatedUser;
import podcastService.user.dto.UpdateUserProfileRequest;
import podcastService.user.dto.UpdateUserSettingsRequest;
import podcastService.user.dto.UserProfilePrivateResponse;
import podcastService.user.dto.UserSettingsResponse;
import podcastService.user.service.UserProfileService;

@Slf4j
@RestController
@RequestMapping("/users/me")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class UserProfileController {
    private final UserProfileService userProfileService;

    @GetMapping("/profile")
    public UserProfilePrivateResponse getUserProfile(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        log.info("GET /users/me/profile, userId={}", currentUser.userId());
        return userProfileService.findUserByUserId(currentUser.userId());
    }

    @PutMapping("/profile")
    public UserProfilePrivateResponse updateUserProfile(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        log.info("PUT /users/me/profile, userId={}, usernameChanged={}, avatarChanged={}",
                currentUser.userId(),
                request.username() != null,
                request.avatarUrl() != null
        );
        return userProfileService.updateUserProfile(currentUser.userId(), request);
    }

    @GetMapping("/settings")
    public UserSettingsResponse getUserSettings(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        log.info("GET /users/me/settings, userId={}", currentUser.userId());
        return userProfileService.getUserSettings(currentUser.userId());
    }

    @PutMapping("/settings")
    public UserSettingsResponse updateUserSettings(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody UpdateUserSettingsRequest request
    ) {
        log.info("PUT /users/me/settings, userId={}, theme={}, language={}",
                currentUser.userId(), request.theme(), request.language());
        return userProfileService.updateUserSettings(currentUser.userId(), request);
    }
}
