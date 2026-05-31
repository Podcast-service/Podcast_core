package podcastService.user.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import podcastService.common.dto.PageResponse;
import podcastService.infrastructure.security.AuthenticatedUser;
import podcastService.podcast.dto.LikedPodcastsSort;
import podcastService.podcast.dto.PodcastCard;
import podcastService.podcast.service.PodcastService;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/users/me/liked-podcasts")
@PreAuthorize("isAuthenticated()")
public class UserLikedPodcastController {

    private final PodcastService podcastService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public PageResponse<PodcastCard> getLikedPodcasts(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size,
            @RequestParam(required = false) LikedPodcastsSort sort
    ) {
        log.info("GET /users/me/liked-podcasts, currentUserId={}, sort={}, page={}, size={}",
                currentUser.userId(), sort, page, size);
        return podcastService.listLikedByCurrentUser(currentUser.userId(), sort, page, size);
    }
}
