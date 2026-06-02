package podcastService.transcript.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PodcastTranscriptResponse(
        UUID podcastId,
        String language,
        JsonNode content,
        OffsetDateTime generatedAt
) {
}
