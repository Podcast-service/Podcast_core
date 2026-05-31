package podcastService.media.messaging.contract;

import com.fasterxml.jackson.annotation.JsonAlias;
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
        @JsonAlias({"audioUrl", "hls_url", "hlsUrl"})
        @JsonProperty("audio_url")
        String audioUrl,
        @JsonAlias({"durationSeconds", "duration", "audio_duration_seconds"})
        @JsonProperty("duration_seconds")
        @JsonDeserialize(using = KafkaLongDeserializer.class)
        Long durationSeconds,
        @JsonAlias({"audioFileSize", "audio_size_file", "audioSizeFile"})
        @JsonProperty("audio_file_size")
        @JsonDeserialize(using = KafkaLongDeserializer.class)
        Long audioFileSize,
        @JsonAlias("error_message")
        String error,
        OffsetDateTime timestamp
) {
    public MediaWorkerEventType normalizedEvent() {
        if (event != null) {
            return event;
        }
        if (audioUrl != null && !audioUrl.isBlank()) {
            return MediaWorkerEventType.PROCESSED;
        }
        return error == null || error.isBlank() ? null : MediaWorkerEventType.PROCESSING_FAILED;
    }

    public MediaObjectType normalizedObjectType() {
        if (objectType != null) {
            return objectType;
        }
        return normalizedEvent() == null ? null : MediaObjectType.PODCAST_FILE_URL;
    }

    public UUID targetPodcastId() {
        return podcastId != null ? podcastId : objectId;
    }

    public UUID targetPodcastId(UUID fallbackPodcastId) {
        UUID target = targetPodcastId();
        return target != null ? target : fallbackPodcastId;
    }
}
