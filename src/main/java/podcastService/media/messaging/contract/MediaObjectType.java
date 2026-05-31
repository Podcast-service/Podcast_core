package podcastService.media.messaging.contract;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import podcastService.infrastructure.messaging.error.InvalidKafkaMessageException;

import java.util.Locale;
import java.util.Arrays;

public enum MediaObjectType {
    PLAYLIST("playlist"),
    PODCAST_FILE_URL("podcast_file_url"),
    PODCAST_COVER_URL("podcast_cover_url"),
    AVATAR("avatar");

    private final String value;

    MediaObjectType(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static MediaObjectType fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidKafkaMessageException("Unsupported object type: " + value);
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        MediaObjectType alias = switch (normalized) {
            case "playlist", "playlists" -> PLAYLIST;
            case "podcast_file_url", "podcast_file", "podcast_audio", "audio" -> PODCAST_FILE_URL;
            case "podcast_cover_url", "podcast_cover", "cover" -> PODCAST_COVER_URL;
            case "avatar", "avatar_url" -> AVATAR;
            default -> null;
        };
        if (alias != null) {
            return alias;
        }

        return Arrays.stream(values())
                .filter(type -> type.value.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new InvalidKafkaMessageException("Unsupported object type: " + value));
    }
}
