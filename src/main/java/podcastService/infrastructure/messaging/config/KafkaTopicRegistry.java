package podcastService.infrastructure.messaging.config;

import org.springframework.stereotype.Component;

@Component
public class KafkaTopicRegistry {

    private final KafkaTopicsProperties properties;

    public KafkaTopicRegistry(KafkaTopicsProperties properties) {
        this.properties = properties;
    }

    public String topic(KafkaTopicDomain domain, KafkaTopicEvent event) {
        return properties.getTopic(domain, event);
    }
}
