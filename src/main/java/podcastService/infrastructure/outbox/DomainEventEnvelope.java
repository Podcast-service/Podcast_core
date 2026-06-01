package podcastService.infrastructure.outbox;

import java.time.Instant;
import java.util.UUID;

public record DomainEventEnvelope(
        UUID eventId,
        String eventType,
        int eventVersion,
        String producer,
        Instant occurredAt,
        String correlationId,
        String causationId,
        UUID userId,
        Object payload
) {
}
