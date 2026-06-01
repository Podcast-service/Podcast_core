package podcastService.infrastructure.outbox.recommendation;

import podcastService.infrastructure.outbox.DomainEventEnvelope;
import podcastService.infrastructure.outbox.recommendation.payload.PodcastDeletedPayload;
import podcastService.infrastructure.outbox.recommendation.payload.PodcastPublishedPayload;
import podcastService.infrastructure.outbox.recommendation.payload.PodcastUpdatedPayload;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class PodcastContentEventFactory {

    private PodcastContentEventFactory() {
    }

    public static DomainEventEnvelope published(
            UUID podcastId,
            UUID authorId,
            UUID categoryId,
            String title,
            String description,
            Long durationSeconds,
            Instant publishedAt,
            String language,
            List<String> tags,
            String status,
            Boolean isExplicit,
            Instant occurredAt,
            UUID userId,
            String correlationId,
            String causationId
    ) {
        Instant eventTime = RecommendationEventEnvelopeFactory.occurredAtOrNow(occurredAt);
        return RecommendationEventEnvelopeFactory.create(
                RecommendationEventTypes.PODCAST_PUBLISHED,
                eventTime,
                correlationId,
                causationId,
                userId,
                new PodcastPublishedPayload(
                        podcastId,
                        authorId,
                        categoryId,
                        title,
                        description,
                        durationSeconds,
                        publishedAt,
                        language,
                        tags,
                        status,
                        isExplicit
                )
        );
    }

    public static DomainEventEnvelope updated(
            UUID podcastId,
            UUID authorId,
            UUID categoryId,
            String title,
            String description,
            Long durationSeconds,
            Instant publishedAt,
            String language,
            List<String> tags,
            String status,
            Boolean isExplicit,
            Instant updatedAt,
            Instant occurredAt,
            UUID userId,
            String correlationId,
            String causationId
    ) {
        Instant eventTime = RecommendationEventEnvelopeFactory.occurredAtOrNow(occurredAt);
        return RecommendationEventEnvelopeFactory.create(
                RecommendationEventTypes.PODCAST_UPDATED,
                eventTime,
                correlationId,
                causationId,
                userId,
                new PodcastUpdatedPayload(
                        podcastId,
                        authorId,
                        categoryId,
                        title,
                        description,
                        durationSeconds,
                        publishedAt,
                        language,
                        tags,
                        status,
                        isExplicit,
                        updatedAt
                )
        );
    }

    public static DomainEventEnvelope deleted(
            UUID podcastId,
            UUID authorId,
            UUID categoryId,
            Instant occurredAt,
            UUID userId,
            String correlationId,
            String causationId
    ) {
        Instant eventTime = RecommendationEventEnvelopeFactory.occurredAtOrNow(occurredAt);
        return RecommendationEventEnvelopeFactory.create(
                RecommendationEventTypes.PODCAST_DELETED,
                eventTime,
                correlationId,
                causationId,
                userId,
                new PodcastDeletedPayload(
                        podcastId,
                        authorId,
                        categoryId,
                        eventTime,
                        RecommendationEventPayloadValues.DELETED_STATUS
                )
        );
    }
}
