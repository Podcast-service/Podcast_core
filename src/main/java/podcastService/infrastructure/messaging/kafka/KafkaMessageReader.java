package podcastService.infrastructure.messaging.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import podcastService.infrastructure.messaging.error.KafkaDeserializationException;
import podcastService.infrastructure.messaging.error.KafkaMessageValidationException;

@Component
@RequiredArgsConstructor
public class KafkaMessageReader {

    private final ObjectMapper objectMapper;

    public <T> T read(String rawPayload, Class<T> dtoType, KafkaRecordContext context) {
        String payload = normalizePayload(rawPayload);
        if (payload == null || payload.isBlank()) {
            throw new KafkaMessageValidationException("Kafka message payload is blank");
        }
        try {
            return objectMapper.readValue(payload, dtoType);
        } catch (JsonProcessingException exception) {
            throw new KafkaDeserializationException(
                    "Failed to deserialize Kafka payload, topic=" + context.topic()
                            + ", partition=" + context.partition()
                            + ", offset=" + context.offset()
                            + ", dtoType=" + dtoType.getSimpleName(),
                    exception
            );
        }
    }

    private String normalizePayload(String rawPayload) {
        if (rawPayload == null) {
            return null;
        }
        String payload = rawPayload.stripLeading();
        while (payload.startsWith("\uFEFF")) {
            payload = payload.substring(1).stripLeading();
        }
        return payload;
    }
}
