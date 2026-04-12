package podcastService.infrastructure.messaging.event;

import java.time.Instant;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventEnvelopeFactory {

    private final ObjectMapper objectMapper;

    public EventEnvelope create(EventType eventType, Object payload) {
        return new EventEnvelope(
                eventType,
                Instant.now(),
                objectMapper.valueToTree(payload)
        );
    }
}
