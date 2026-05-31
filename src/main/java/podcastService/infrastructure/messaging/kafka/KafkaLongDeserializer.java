package podcastService.infrastructure.messaging.kafka;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import podcastService.infrastructure.messaging.error.InvalidKafkaMessageException;

import java.io.IOException;

public class KafkaLongDeserializer extends JsonDeserializer<Long> {

    private static final String INTEGER_PATTERN = "-?\\d+";

    @Override
    public Long deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String value = parser.getValueAsString();
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();
        if (!normalized.matches(INTEGER_PATTERN)) {
            throw new InvalidKafkaMessageException("Kafka numeric field has invalid integer value: " + value);
        }

        try {
            return Long.parseLong(normalized);
        } catch (NumberFormatException exception) {
            throw new InvalidKafkaMessageException("Kafka numeric field is out of long range: " + value, exception);
        }
    }
}
