package podcastService.infrastructure.outbox.recommendation.payload;

import java.time.Instant;
import java.util.UUID;

public record PodcastDislikedPayload(
        UUID podcastId,
        UUID userId,
        UUID authorId,
        UUID categoryId,
        Instant occurredAt,
        Instant dislikedAt
) {
}
