package podcastService.subscription.dto;

import podcastService.author.dto.AuthorCard;

import java.time.OffsetDateTime;

public record SubscriptionResponse(
        AuthorCard author,
        OffsetDateTime subscribedAt
) {
}
