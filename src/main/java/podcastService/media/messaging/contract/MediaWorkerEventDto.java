package podcastService.media.messaging.contract;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MediaWorkerEventDto(
        @JsonProperty("object_type")
        MediaObjectType objectType,
        @JsonProperty("object_id")
        UUID objectId,
        MediaWorkerEventType event,
        @JsonProperty("podcast_id")
        UUID podcastId,
        @JsonAlias({"audioUrl", "hls_url", "hlsUrl"})
        @JsonProperty("audio_url")
        String audioUrl,
        @JsonAlias({"durationSeconds", "duration", "audio_duration_seconds"})
        @JsonProperty("duration_seconds")
        Long durationSeconds,
        @JsonAlias({"audioFileSize", "audio_size_file", "audioSizeFile"})
        @JsonProperty("audio_file_size")
        Long audioFileSize,
        String error,
        OffsetDateTime timestamp
) {
    public MediaWorkerEventType normalizedEvent() {
        if (event != null) {
            return event;
        }
        return error == null || error.isBlank() ? null : MediaWorkerEventType.PROCESSING_FAILED;
    }

    public MediaObjectType normalizedObjectType() {
        if (objectType != null) {
            return objectType;
        }
        return normalizedEvent() == MediaWorkerEventType.PROCESSING_FAILED ? MediaObjectType.PODCAST_FILE_URL : null;
    }

    public UUID targetPodcastId() {
        return podcastId != null ? podcastId : objectId;
    }
}
