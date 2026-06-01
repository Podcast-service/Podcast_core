package podcastService.infrastructure.outbox.recommendation.payload;

import java.time.Instant;
import java.util.UUID;

public record PodcastDeletedPayload(
        UUID podcastId,
        UUID authorId,
        Instant deletedAt
) {
}
