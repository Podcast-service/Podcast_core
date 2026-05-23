package podcastService.subscription.dto;

import java.util.UUID;

public record AuthorSubscriptionResponse(
        UUID authorId,
        Long subscribersCount,
        Boolean isSubscribed
) {
}
