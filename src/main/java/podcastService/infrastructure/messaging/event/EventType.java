package podcastService.infrastructure.messaging.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EventType {
    USER_CREATED("user.created");

    private final String code;

    EventType(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static EventType fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        for (EventType type : values()) {
            if (type.code.equalsIgnoreCase(value) || type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }

        throw new IllegalArgumentException("Unknown event type: " + value);
    }
}
