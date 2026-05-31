package podcastService.media.messaging.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MediaSubtitleEventDto(
        @JsonProperty("podcast_id")
        UUID podcastId,
        JsonNode content,
        @JsonProperty("ready_at")
        OffsetDateTime readyAt
) {
}
