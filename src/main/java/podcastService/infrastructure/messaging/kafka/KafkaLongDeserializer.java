package podcastService.infrastructure.messaging.kafka;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import podcastService.infrastructure.messaging.error.InvalidKafkaMessageException;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class KafkaLongDeserializer extends JsonDeserializer<Long> {

    @Override
    public Long deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String value = parser.getValueAsString();
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            BigDecimal decimal = new BigDecimal(value.trim());
            return decimal.setScale(0, RoundingMode.CEILING).longValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            throw new InvalidKafkaMessageException("Kafka numeric field has invalid long value: " + value, exception);
        }
    }
}
