package podcastService.infrastructure.outbox.recommendation;

import podcastService.infrastructure.outbox.DomainEventEnvelope;

import java.time.Instant;
import java.util.UUID;

final class RecommendationEventEnvelopeFactory {

    private static final int EVENT_VERSION = 1;
    private static final String PRODUCER = "podcast-core";

    private RecommendationEventEnvelopeFactory() {
    }

    static DomainEventEnvelope create(
            String eventType,
            Instant occurredAt,
            String correlationId,
            String causationId,
            UUID userId,
            Object payload
    ) {
        Instant normalizedOccurredAt = occurredAtOrNow(occurredAt);
        return new DomainEventEnvelope(
                UUID.randomUUID(),
                eventType,
                EVENT_VERSION,
                PRODUCER,
                normalizedOccurredAt,
                blankToNull(correlationId),
                blankToNull(causationId),
                userId,
                payload
        );
    }

    static Instant occurredAtOrNow(Instant occurredAt) {
        return occurredAt == null ? Instant.now() : occurredAt;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
