package podcastService.infrastructure.messaging.producer;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import podcastService.infrastructure.messaging.config.KafkaEventRoutingRegistry;
import podcastService.infrastructure.messaging.event.EventEnvelope;
import podcastService.infrastructure.messaging.event.EventEnvelopeFactory;
import podcastService.infrastructure.messaging.event.EventType;

@Component
@RequiredArgsConstructor
public class KafkaDomainEventProducer {

    private final KafkaEventRoutingRegistry kafkaEventRoutingRegistry;
    private final EventEnvelopeFactory eventEnvelopeFactory;
    private final KafkaEventPublisher kafkaEventPublisher;

    public void publish(EventType eventType, Object payload, String key) {
        EventEnvelope event = eventEnvelopeFactory.create(eventType, payload);
        String topic = kafkaEventRoutingRegistry.resolveTopic(eventType);

        kafkaEventPublisher.publish(topic, key, event);
    }
}
