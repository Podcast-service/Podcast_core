package podcastService.media.messaging.contract;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import podcastService.infrastructure.messaging.kafka.KafkaLongDeserializer;

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
        @JsonProperty("audio_url")
        String audioUrl,
        @JsonProperty("duration_seconds")
        @JsonDeserialize(using = KafkaLongDeserializer.class)
        Long durationSeconds,
        @JsonProperty("audio_file_size")
        @JsonDeserialize(using = KafkaLongDeserializer.class)
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
        return normalizedEvent() == MediaWorkerEventType.PROCESSING_FAILED
                || normalizedEvent() == MediaWorkerEventType.ERROR
                ? MediaObjectType.PODCAST_FILE_URL
                : null;
    }

    public UUID targetPodcastId() {
        return podcastId != null ? podcastId : objectId;
    }
}
