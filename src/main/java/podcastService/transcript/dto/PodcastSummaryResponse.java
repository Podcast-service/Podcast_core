package podcastService.transcript.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PodcastSummaryResponse(
        UUID podcastId,
        String language,
        String content,
        OffsetDateTime generatedAt
) {
}
