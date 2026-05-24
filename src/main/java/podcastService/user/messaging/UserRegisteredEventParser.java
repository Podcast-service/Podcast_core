package podcastService.user.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import podcastService.infrastructure.messaging.error.KafkaMessageValidationException;

import java.util.UUID;

@Component
public class UserRegisteredEventParser {

    public UserRegisteredEvent parse(JsonNode node) {
        UUID userId = requiredUuid(node, "user_id");
        String username = requiredText(node, "username");
        return new UserRegisteredEvent(userId, username);
    }

    private UUID requiredUuid(JsonNode node, String field) {
        String value = requiredText(node, field);
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new KafkaMessageValidationException("Invalid UUID field in user registration event: " + field);
        }
    }

    private String requiredText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || !value.isTextual() || value.asText().isBlank()) {
            throw new KafkaMessageValidationException("Missing or blank field in user registration event: " + field);
        }
        return value.asText().trim();
    }
}
