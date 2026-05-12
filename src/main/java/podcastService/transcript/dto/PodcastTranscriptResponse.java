package podcastService.transcript.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PodcastTranscriptResponse(
        UUID podcastId,
        String language,
        String content,
        OffsetDateTime generatedAt
) {
}
