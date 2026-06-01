package podcastService.infrastructure.outbox.recommendation;

import podcastService.infrastructure.outbox.DomainEventEnvelope;
import podcastService.infrastructure.outbox.recommendation.payload.AuthorFollowedPayload;
import podcastService.infrastructure.outbox.recommendation.payload.AuthorUnfollowedPayload;

import java.time.Instant;
import java.util.UUID;

public final class AuthorActivityEventFactory {

    private AuthorActivityEventFactory() {
    }

    public static DomainEventEnvelope followed(
            UUID authorId,
            UUID userId,
            Instant occurredAt,
            String correlationId,
            String causationId
    ) {
        Instant eventTime = RecommendationEventEnvelopeFactory.occurredAtOrNow(occurredAt);
        return RecommendationEventEnvelopeFactory.create(
                RecommendationEventTypes.AUTHOR_FOLLOWED,
                eventTime,
                correlationId,
                causationId,
                userId,
                new AuthorFollowedPayload(authorId, userId, eventTime, eventTime)
        );
    }

    public static DomainEventEnvelope unfollowed(
            UUID authorId,
            UUID userId,
            Instant occurredAt,
            String correlationId,
            String causationId
    ) {
        Instant eventTime = RecommendationEventEnvelopeFactory.occurredAtOrNow(occurredAt);
        return RecommendationEventEnvelopeFactory.create(
                RecommendationEventTypes.AUTHOR_UNFOLLOWED,
                eventTime,
                correlationId,
                causationId,
                userId,
                new AuthorUnfollowedPayload(authorId, userId, eventTime, eventTime)
        );
    }
}
