package podcastService.media.messaging.contract;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MediaUploadEventDto(
        @JsonProperty("object_type")
        MediaObjectType objectType,
        @JsonProperty("object_id")
        UUID objectId,
        MediaUploadEventType event,
        @JsonProperty("podcast_id")
        UUID podcastId,
        @JsonProperty("audio_url_file")
        String audioUrlFile,
        @JsonProperty("audio_file_size")
        Long audioFileSize,
        String error,
        OffsetDateTime timestamp
) {
    public MediaUploadEventType normalizedEvent() {
        if (event != null) {
            return event;
        }
        return error == null || error.isBlank() ? null : MediaUploadEventType.UPLOAD_FAILED;
    }

    public MediaObjectType normalizedObjectType() {
        if (objectType != null) {
            return objectType;
        }
        return normalizedEvent() == MediaUploadEventType.UPLOAD_FAILED ? MediaObjectType.PODCAST_FILE_URL : null;
    }

    public UUID targetPodcastId() {
        return podcastId != null ? podcastId : objectId;
    }
}
