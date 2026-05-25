package podcastService.media.messaging.contract;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TtsStartEventDto(
        @JsonProperty("podcast_id")
        UUID podcastId,
        String content,
        OffsetDateTime timestamp
) {
}
