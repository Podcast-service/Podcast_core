package podcastService.infrastructure.messaging.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

public record KafkaRecordContext(
        String topic,
        int partition,
        long offset,
        String key,
        String correlationId,
        String messageId
) {
    public static KafkaRecordContext from(ConsumerRecord<String, String> record) {
        return new KafkaRecordContext(
                record.topic(),
                record.partition(),
                record.offset(),
                record.key(),
                firstHeader(record, "correlation_id")
                        .or(() -> firstHeader(record, "correlationId"))
                        .or(() -> firstHeader(record, "request_id"))
                        .orElse(null),
                firstHeader(record, "message_id")
                        .or(() -> firstHeader(record, "messageId"))
                        .orElse(null)
        );
    }

    private static Optional<String> firstHeader(ConsumerRecord<String, String> record, String name) {
        Header header = record.headers().lastHeader(name);
        if (header == null || header.value() == null) {
            return Optional.empty();
        }
        return Optional.of(new String(header.value(), StandardCharsets.UTF_8));
    }

    public UUID keyAsUuidOrNull() {
        if (key == null || key.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(key);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
