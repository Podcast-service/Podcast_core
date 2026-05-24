package podcastService.media.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import podcastService.infrastructure.messaging.error.InvalidKafkaMessageException;

import java.util.UUID;

public record MediaEvent(
        String type,
        String event,
        UUID objectId,
        JsonNode rawPayload
) {
    public MediaEventKey key() {
        return new MediaEventKey(type, event);
    }

    public String optionalText(String field) {
        JsonNode node = rawPayload.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        if (!node.isTextual()) {
            return node.asText(null);
        }
        String value = node.asText();
        return value == null || value.isBlank() ? null : value.trim();
    }

    public String requiredText(String field) {
        String value = optionalText(field);
        if (value == null) {
            throw new InvalidKafkaMessageException("Media event has missing or blank field: " + field);
        }
        return value;
    }

    public String requiredAnyText(String... fields) {
        for (String field : fields) {
            String value = optionalText(field);
            if (value != null) {
                return value;
            }
        }
        throw new InvalidKafkaMessageException("Media event has missing or blank field. Expected one of: "
                + String.join(", ", fields));
    }

    public Long optionalLong(String field) {
        JsonNode node = rawPayload.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.longValue();
        }
        if (node.isTextual()) {
            String value = node.asText();
            if (value == null || value.isBlank()) {
                return null;
            }
            try {
                return Long.parseLong(value.trim());
            } catch (NumberFormatException exception) {
                throw new InvalidKafkaMessageException("Media event has invalid long field: " + field, exception);
            }
        }
        throw new InvalidKafkaMessageException("Media event has invalid long field: " + field);
    }
}
