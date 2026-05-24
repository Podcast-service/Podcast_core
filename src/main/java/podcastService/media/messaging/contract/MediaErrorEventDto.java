package podcastService.media.messaging.contract;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MediaErrorEventDto(
        @JsonProperty("podcast_id")
        UUID podcastId,
        @JsonProperty("object_type")
        MediaObjectType objectType,
        @JsonProperty("object_id")
        UUID objectId,
        String event,
        String error,
        OffsetDateTime timestamp
) {
}
