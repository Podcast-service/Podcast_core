package podcastService.infrastructure.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.recommendation.events")
public record RecommendationEventsProperties(
        boolean enabled
) {
}
