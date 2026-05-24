package podcastService.media.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import podcastService.infrastructure.messaging.error.KafkaMessageValidationException;

import java.util.UUID;

@Component
public class MediaEventParser {

    public MediaEvent parse(JsonNode payload) {
        String type = requiredText(payload, "type");
        String event = requiredText(payload, "event");
        UUID objectId = requiredUuid(payload, "object_id");
        return new MediaEvent(type, event, objectId, payload);
    }

    private UUID requiredUuid(JsonNode payload, String field) {
        String value = requiredText(payload, field);
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new KafkaMessageValidationException("Media event has invalid UUID field: " + field);
        }
    }

    private String requiredText(JsonNode payload, String field) {
        JsonNode value = payload.get(field);
        if (value == null || value.isNull() || !value.isTextual() || value.asText().isBlank()) {
            throw new KafkaMessageValidationException("Media event has missing or blank field: " + field);
        }
        return value.asText().trim();
    }
}
