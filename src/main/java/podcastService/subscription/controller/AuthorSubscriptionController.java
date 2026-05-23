package podcastService.subscription.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import podcastService.infrastructure.security.AuthenticatedUser;
import podcastService.subscription.dto.AuthorSubscriptionResponse;
import podcastService.subscription.service.SubscriptionService;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/authors/{authorId}/subscribe")
public class AuthorSubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.OK)
    public AuthorSubscriptionResponse subscribeAuthor(
            @PathVariable UUID authorId,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        UUID currentUserId = currentUser.userId();
        log.info("POST /authors/{}/subscribe, currentUserId={}", authorId, currentUserId);
        return subscriptionService.subscribe(authorId, currentUserId);
    }

    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.OK)
    public AuthorSubscriptionResponse unsubscribeAuthor(
            @PathVariable UUID authorId,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        UUID currentUserId = currentUser.userId();
        log.info("DELETE /authors/{}/subscribe, currentUserId={}", authorId, currentUserId);
        return subscriptionService.unsubscribe(authorId, currentUserId);
    }
}
