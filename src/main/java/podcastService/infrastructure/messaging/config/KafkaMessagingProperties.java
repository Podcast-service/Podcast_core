package podcastService.infrastructure.messaging.config;

import jakarta.validation.constraints.NotEmpty;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.kafka")
public class KafkaMessagingProperties {

    @NotEmpty
    private Map<String, String> topics = new HashMap<>();

    @NotEmpty
    private Map<String, String> routing = new HashMap<>();
}
