package podcastService.media.messaging.contract;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import podcastService.infrastructure.messaging.error.InvalidKafkaMessageException;

import java.util.Arrays;

public enum MediaUploadEventType {
    START_UPLOAD("start_upload"),
    UPLOADED("uploaded"),
    UPLOAD_FAILED("upload_failed"),
    ERROR("error");

    private final String value;

    MediaUploadEventType(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static MediaUploadEventType fromValue(String value) {
        return Arrays.stream(values())
                .filter(type -> type.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new InvalidKafkaMessageException("Unsupported media.upload event: " + value));
    }
}
