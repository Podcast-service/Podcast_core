package podcastService.infrastructure.outbox.recommendation.payload;

import java.time.Instant;
import java.util.UUID;

public record PodcastLikedPayload(
        UUID podcastId,
        UUID userId,
        Instant likedAt
) {
}
