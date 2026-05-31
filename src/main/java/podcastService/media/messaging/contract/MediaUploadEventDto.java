package podcastService.media.messaging.contract;

import com.fasterxml.jackson.annotation.JsonAlias;
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
        @JsonAlias({"audio_file_url", "audioUrlFile"})
        @JsonProperty("audio_url_file")
        String audioUrlFile,
        @JsonAlias({"imageUrl", "cover_url", "avatar_url"})
        @JsonProperty("image_url")
        String imageUrl,
        String error,
        OffsetDateTime timestamp
) {
    public MediaUploadEventType normalizedEvent() {
        if (event != null) {
            return event;
        }
        if (error != null && !error.isBlank()) {
            return MediaUploadEventType.UPLOAD_FAILED;
        }
        if (objectType == MediaObjectType.PODCAST_COVER_URL
                || objectType == MediaObjectType.AVATAR
                || objectType == MediaObjectType.PLAYLIST) {
            return MediaUploadEventType.UPLOADED;
        }
        return null;
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

    public String uploadedImageUrl() {
        return imageUrl;
    }
}
