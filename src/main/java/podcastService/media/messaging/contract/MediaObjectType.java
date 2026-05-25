package podcastService.media.messaging.contract;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

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
        return Arrays.stream(values())
                .filter(type -> type.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported object_type: " + value));
    }
}
