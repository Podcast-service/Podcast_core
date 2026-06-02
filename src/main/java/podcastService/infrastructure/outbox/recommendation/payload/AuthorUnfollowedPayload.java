package podcastService.infrastructure.outbox.recommendation.payload;

import java.time.Instant;
import java.util.UUID;

public record AuthorUnfollowedPayload(
        UUID authorId,
        UUID userId,
        Instant occurredAt,
        Instant unfollowedAt
) {
}
