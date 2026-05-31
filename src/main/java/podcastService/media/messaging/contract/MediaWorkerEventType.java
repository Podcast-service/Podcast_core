package podcastService.media.messaging.contract;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import podcastService.infrastructure.messaging.error.InvalidKafkaMessageException;

import java.util.Arrays;

public enum MediaWorkerEventType {
    START_PROCESSING("start_processing"),
    PROCESSED("processed"),
    PROCESSING_FAILED("processing_failed"),
    ERROR("error");

    private final String value;

    MediaWorkerEventType(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static MediaWorkerEventType fromValue(String value) {
        return Arrays.stream(values())
                .filter(type -> type.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new InvalidKafkaMessageException("Unsupported media.worker event: " + value));
    }
}
