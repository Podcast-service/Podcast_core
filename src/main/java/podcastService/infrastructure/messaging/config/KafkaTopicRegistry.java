package podcastService.infrastructure.messaging.config;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class KafkaTopicRegistry {

    private final Map<String, String> topics;

    public KafkaTopicRegistry(KafkaMessagingProperties properties) {
        this.topics = Map.copyOf(properties.getTopics());
    }

    public String getRequiredTopic(String logicalTopicName) {
        String topic = topics.get(logicalTopicName);

        if (topic == null || topic.isBlank()) {
            throw new IllegalStateException(
                    "Kafka topic configuration is missing for logical topic: " + logicalTopicName
            );
        }

        return topic;
    }
}
