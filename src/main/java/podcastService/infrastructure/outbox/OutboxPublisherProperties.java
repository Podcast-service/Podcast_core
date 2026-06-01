package podcastService.infrastructure.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.outbox.publisher")
public record OutboxPublisherProperties(
        boolean enabled,
        int batchSize,
        long publishDelayMs,
        int maxRetryAttempts,
        long processingTimeoutMs,
        long sendTimeoutMs
) {
}
