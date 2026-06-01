package podcastService.infrastructure.outbox.recommendation.payload;

import java.time.Instant;
import java.util.UUID;

public record AuthorFollowedPayload(
        UUID authorId,
        UUID userId,
        Instant followedAt
) {
}
