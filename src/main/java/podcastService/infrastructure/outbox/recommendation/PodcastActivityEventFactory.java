package podcastService.infrastructure.outbox.recommendation;

import podcastService.infrastructure.outbox.DomainEventEnvelope;
import podcastService.infrastructure.outbox.recommendation.payload.PodcastDislikedPayload;
import podcastService.infrastructure.outbox.recommendation.payload.PodcastLikedPayload;
import podcastService.infrastructure.outbox.recommendation.payload.PodcastPlayFinishedPayload;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

public final class PodcastActivityEventFactory {

    private PodcastActivityEventFactory() {
    }

    public static DomainEventEnvelope playFinished(
            UUID podcastId,
            UUID userId,
            UUID authorId,
            UUID categoryId,
            Long durationSeconds,
            long progressSeconds,
            String source,
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
                new PodcastPlayFinishedPayload(
                        podcastId,
                        userId,
                        authorId,
                        categoryId,
                        durationSeconds,
                        progressSeconds,
                        progressPercent(progressSeconds, durationSeconds),
                        source,
                        eventTime,
                        eventTime
                )
        );
    }

    public static DomainEventEnvelope liked(
            UUID podcastId,
            UUID userId,
            UUID authorId,
            UUID categoryId,
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
                new PodcastLikedPayload(podcastId, userId, authorId, categoryId, eventTime, eventTime)
        );
    }

    public static DomainEventEnvelope disliked(
            UUID podcastId,
            UUID userId,
            UUID authorId,
            UUID categoryId,
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
                new PodcastDislikedPayload(podcastId, userId, authorId, categoryId, eventTime, eventTime)
        );
    }

    private static BigDecimal progressPercent(long progressSeconds, Long durationSeconds) {
        if (durationSeconds == null || durationSeconds <= 0) {
            return null;
        }
        return BigDecimal.valueOf(progressSeconds)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(durationSeconds), 2, RoundingMode.HALF_UP)
                .min(BigDecimal.valueOf(100));
    }
}
