package podcastService.infrastructure.messaging.event;



import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

public record EventEnvelope(
        EventType eventType,
        Instant occurredAt,
        JsonNode payload
) {
}
