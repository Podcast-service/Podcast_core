package podcastService.infrastructure.outbox.recommendation.payload;

import java.time.Instant;
import java.util.UUID;

public record PodcastPlayFinishedPayload(
        UUID podcastId,
        UUID userId,
        long progressSeconds,
        Instant finishedAt
) {
}
