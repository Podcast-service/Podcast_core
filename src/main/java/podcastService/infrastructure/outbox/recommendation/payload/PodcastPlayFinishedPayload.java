package podcastService.infrastructure.outbox.recommendation.payload;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PodcastPlayFinishedPayload(
        UUID podcastId,
        UUID userId,
        UUID authorId,
        UUID categoryId,
        Long durationSeconds,
        long progressSeconds,
        BigDecimal progressPercent,
        String source,
        Instant occurredAt,
        Instant finishedAt
) {
}
