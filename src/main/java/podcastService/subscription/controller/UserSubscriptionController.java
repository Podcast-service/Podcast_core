package podcastService.subscription.controller;

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
import podcastService.podcast.dto.PodcastCard;
import podcastService.podcast.dto.SortPodcasts;
import podcastService.subscription.dto.SubscriptionResponse;
import podcastService.subscription.service.SubscriptionService;

import java.util.UUID;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@RequestMapping("/users/me/subscriptions")
public class UserSubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public PageResponse<SubscriptionResponse> getMySubscriptions(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size
    ) {
        UUID currentUserId = currentUser.userId();
        log.info("GET /users/me/subscriptions, currentUserId={}, page={}, size={}", currentUserId, page, size);
        return subscriptionService.listMine(currentUserId, page, size);
    }

    @GetMapping("/feed")
    @ResponseStatus(HttpStatus.OK)
    public PageResponse<PodcastCard> getSubscriptionFeed(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(required = false) SortPodcasts sort,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size
    ) {
        UUID currentUserId = currentUser.userId();
        log.info(
                "GET /users/me/subscriptions/feed, currentUserId={}, sort={}, page={}, size={}",
                currentUserId,
                sort,
                page,
                size
        );
        return subscriptionService.feed(currentUserId, page, size, sort);
    }
}
