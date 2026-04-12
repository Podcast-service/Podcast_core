package podcastService.infrastructure.messaging.config;

import java.util.Map;
import org.springframework.stereotype.Component;
import podcastService.infrastructure.messaging.event.EventType;

@Component
public class KafkaEventRoutingRegistry {

    private final Map<String, String> routing;
    private final KafkaTopicRegistry kafkaTopicRegistry;

    public KafkaEventRoutingRegistry(
            KafkaMessagingProperties properties,
            KafkaTopicRegistry kafkaTopicRegistry
    ) {
        this.routing = Map.copyOf(properties.getRouting());
        this.kafkaTopicRegistry = kafkaTopicRegistry;
    }

    public String resolveTopic(EventType eventType) {
        if (eventType == null) {
            throw new IllegalArgumentException("eventType must not be null");
        }

        String logicalTopicName = routing.get(eventType.getCode());

        if (logicalTopicName == null || logicalTopicName.isBlank()) {
            throw new IllegalStateException(
                    "Kafka routing configuration is missing for eventType: " + eventType.getCode()
            );
        }

        return kafkaTopicRegistry.getRequiredTopic(logicalTopicName);
    }
}
