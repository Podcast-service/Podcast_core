package podcastService.infrastructure.outbox.recommendation.payload;

import java.time.Instant;
import java.util.UUID;

public record PodcastPublishedPayload(
        UUID podcastId,
        UUID authorId,
        UUID categoryId,
        String title,
        Instant publishedAt
) {
}
