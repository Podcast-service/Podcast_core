package podcastService.infrastructure.messaging.producer;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import podcastService.infrastructure.messaging.event.EventEnvelope;

@Component
@RequiredArgsConstructor
public class KafkaEventPublisher {

    private final KafkaTemplate<String, EventEnvelope> kafkaTemplate;

    public void publish(String topic, String key, EventEnvelope event) {
        kafkaTemplate.send(topic, key, event);
    }
}
