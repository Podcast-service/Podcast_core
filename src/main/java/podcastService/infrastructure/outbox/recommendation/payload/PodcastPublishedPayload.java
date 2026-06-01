package podcastService.infrastructure.outbox.recommendation.payload;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PodcastPublishedPayload(
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
        Boolean isExplicit
) {
}
