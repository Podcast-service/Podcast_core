package podcastService.infrastructure.messaging.config;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.kafka")
public class KafkaTopicsProperties {

    private Map<String, Map<String, String>> topics = new HashMap<>();

    public String getTopic(KafkaTopicDomain domain, KafkaTopicEvent event) {
        Map<String, String> domainTopics = topics.get(domain.value());

        if (domainTopics == null) {
            throw new IllegalStateException(
                    "Kafka topics configuration is missing domain: " + domain.value()
            );
        }

        String topic = domainTopics.get(event.value());

        if (topic == null || topic.isBlank()) {
            throw new IllegalStateException(
                    "Kafka topic configuration is missing for domain='%s', event='%s'"
                            .formatted(domain.value(), event.value())
            );
        }

        return topic;
    }
}
