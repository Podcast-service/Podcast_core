package podcastService.infrastructure.outbox;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DomainEventEnvelope(
        UUID eventId,
        String eventType,
        int eventVersion,
        String producer,
        OffsetDateTime occurredAt,
        String correlationId,
        String causationId,
        UUID userId,
        Object payload
) {
}
