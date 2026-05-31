package podcastService.media.messaging.contract;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import podcastService.infrastructure.messaging.error.InvalidKafkaMessageException;

import java.util.Arrays;
import java.util.Locale;

public enum MediaWorkerEventType {
    START_PROCESSING("start_processing"),
    CONVERTED("converted"),
    PROCESSED("processed"),
    PROCESSING_FAILED("processing_failed"),
    SUBTITLE_READY("subtitle_ready"),
    DELETED("deleted"),
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
        if (value == null || value.isBlank()) {
            throw new InvalidKafkaMessageException("Unsupported media.worker event: " + value);
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        MediaWorkerEventType alias = switch (normalized) {
            case "start_processing", "processing_started", "processing" -> START_PROCESSING;
            case "converted" -> CONVERTED;
            case "processed", "processing_done", "processing_completed", "completed", "done" -> PROCESSED;
            case "processing_failed", "failed" -> PROCESSING_FAILED;
            case "subtitle_ready" -> SUBTITLE_READY;
            case "deleted" -> DELETED;
            case "error" -> ERROR;
            default -> null;
        };
        if (alias != null) {
            return alias;
        }

        return Arrays.stream(values())
                .filter(type -> type.value.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new InvalidKafkaMessageException("Unsupported media.worker event: " + value));
    }
}
