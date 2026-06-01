package podcastService.infrastructure.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import podcastService.infrastructure.outbox.entity.OutboxEventEntity;
import podcastService.infrastructure.outbox.repository.OutboxEventRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxEventService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public OutboxEventEntity saveEvent(
            String aggregateType,
            UUID aggregateId,
            String eventKey,
            DomainEventEnvelope envelope
    ) {
        validateEnvelope(envelope);

        OutboxEventEntity entity = new OutboxEventEntity();
        entity.setId(envelope.eventId());
        entity.setAggregateType(requireText(aggregateType, "aggregateType"));
        entity.setAggregateId(aggregateId);
        entity.setEventType(requireText(envelope.eventType(), "eventType"));
        entity.setEventVersion(envelope.eventVersion());
        entity.setEventKey(normalizeEventKey(eventKey));
        entity.setPayload(serializeEnvelope(envelope));
        entity.setStatus(OutboxEventStatus.NEW);
        entity.setRetryCount(0);

        return outboxEventRepository.saveAndFlush(entity);
    }

    private void validateEnvelope(DomainEventEnvelope envelope) {
        if (envelope == null) {
            throw new IllegalArgumentException("domain event envelope must not be null");
        }
        if (envelope.eventId() == null) {
            throw new IllegalArgumentException("eventId must not be null");
        }
        if (envelope.eventVersion() <= 0) {
            throw new IllegalArgumentException("eventVersion must be positive");
        }
        requireText(envelope.producer(), "producer");
        if (envelope.occurredAt() == null) {
            throw new IllegalArgumentException("occurredAt must not be null");
        }
        if (envelope.payload() == null) {
            throw new IllegalArgumentException("payload must not be null");
        }
    }

    private JsonNode serializeEnvelope(DomainEventEnvelope envelope) {
        try {
            JsonNode node = objectMapper.valueToTree(envelope);
            if (node == null || node.isNull()) {
                throw new IllegalArgumentException("serialized domain event envelope must not be null");
            }
            return node;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Failed to serialize domain event envelope payload", exception);
        }
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private String normalizeEventKey(String eventKey) {
        if (eventKey == null || eventKey.isBlank()) {
            return null;
        }
        return eventKey.trim();
    }
}
