package podcastService.infrastructure.outbox.recommendation;

import podcastService.infrastructure.outbox.DomainEventEnvelope;
import podcastService.infrastructure.outbox.recommendation.payload.PlaylistCreatedPayload;
import podcastService.infrastructure.outbox.recommendation.payload.PlaylistDeletedPayload;
import podcastService.infrastructure.outbox.recommendation.payload.PlaylistUpdatedPayload;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class PlaylistContentEventFactory {

    private PlaylistContentEventFactory() {
    }

    public static DomainEventEnvelope created(
            UUID playlistId,
            UUID ownerUserId,
            String title,
            String description,
            boolean publicPlaylist,
            List<UUID> podcastIds,
            Instant createdAt,
            Instant occurredAt,
            String correlationId,
            String causationId
    ) {
        Instant eventTime = RecommendationEventEnvelopeFactory.occurredAtOrNow(occurredAt);
        return RecommendationEventEnvelopeFactory.create(
                RecommendationEventTypes.PLAYLIST_CREATED,
                eventTime,
                correlationId,
                causationId,
                ownerUserId,
                new PlaylistCreatedPayload(
                        playlistId,
                        ownerUserId,
                        title,
                        description,
                        publicPlaylist,
                        podcastIds,
                        createdAt
                )
        );
    }

    public static DomainEventEnvelope updated(
            UUID playlistId,
            UUID ownerUserId,
            String title,
            String description,
            boolean publicPlaylist,
            List<UUID> podcastIds,
            Instant createdAt,
            Instant updatedAt,
            Instant occurredAt,
            String correlationId,
            String causationId
    ) {
        Instant eventTime = RecommendationEventEnvelopeFactory.occurredAtOrNow(occurredAt);
        return RecommendationEventEnvelopeFactory.create(
                RecommendationEventTypes.PLAYLIST_UPDATED,
                eventTime,
                correlationId,
                causationId,
                ownerUserId,
                new PlaylistUpdatedPayload(
                        playlistId,
                        ownerUserId,
                        title,
                        description,
                        publicPlaylist,
                        podcastIds,
                        createdAt,
                        updatedAt
                )
        );
    }

    public static DomainEventEnvelope deleted(
            UUID playlistId,
            UUID ownerUserId,
            Instant occurredAt,
            String correlationId,
            String causationId
    ) {
        Instant eventTime = RecommendationEventEnvelopeFactory.occurredAtOrNow(occurredAt);
        return RecommendationEventEnvelopeFactory.create(
                RecommendationEventTypes.PLAYLIST_DELETED,
                eventTime,
                correlationId,
                causationId,
                ownerUserId,
                new PlaylistDeletedPayload(
                        playlistId,
                        ownerUserId,
                        eventTime,
                        RecommendationEventPayloadValues.DELETED_STATUS
                )
        );
    }
}
