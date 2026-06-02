package podcastService.transcript.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PodcastTranscriptResponse(
        UUID podcastId,
        String language,
        Object content,
        OffsetDateTime generatedAt
) {
}
