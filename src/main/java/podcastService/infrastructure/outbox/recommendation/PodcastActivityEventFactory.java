package podcastService.infrastructure.outbox.recommendation;

import podcastService.infrastructure.outbox.DomainEventEnvelope;
import podcastService.infrastructure.outbox.recommendation.payload.PodcastDislikedPayload;
import podcastService.infrastructure.outbox.recommendation.payload.PodcastLikedPayload;
import podcastService.infrastructure.outbox.recommendation.payload.PodcastPlayFinishedPayload;

import java.time.Instant;
import java.util.UUID;

public final class PodcastActivityEventFactory {

    private PodcastActivityEventFactory() {
    }

    public static DomainEventEnvelope playFinished(
            UUID podcastId,
            UUID userId,
            long progressSeconds,
            Instant occurredAt,
            String correlationId,
            String causationId
    ) {
        Instant eventTime = RecommendationEventEnvelopeFactory.occurredAtOrNow(occurredAt);
        return RecommendationEventEnvelopeFactory.create(
                RecommendationEventTypes.PODCAST_PLAY_FINISHED,
                eventTime,
                correlationId,
                causationId,
                userId,
                new PodcastPlayFinishedPayload(podcastId, userId, progressSeconds, eventTime)
        );
    }

    public static DomainEventEnvelope liked(
            UUID podcastId,
            UUID userId,
            Instant occurredAt,
            String correlationId,
            String causationId
    ) {
        Instant eventTime = RecommendationEventEnvelopeFactory.occurredAtOrNow(occurredAt);
        return RecommendationEventEnvelopeFactory.create(
                RecommendationEventTypes.PODCAST_LIKED,
                eventTime,
                correlationId,
                causationId,
                userId,
                new PodcastLikedPayload(podcastId, userId, eventTime)
        );
    }

    public static DomainEventEnvelope disliked(
            UUID podcastId,
            UUID userId,
            Instant occurredAt,
            String correlationId,
            String causationId
    ) {
        Instant eventTime = RecommendationEventEnvelopeFactory.occurredAtOrNow(occurredAt);
        return RecommendationEventEnvelopeFactory.create(
                RecommendationEventTypes.PODCAST_DISLIKED,
                eventTime,
                correlationId,
                causationId,
                userId,
                new PodcastDislikedPayload(podcastId, userId, eventTime)
        );
    }
}
