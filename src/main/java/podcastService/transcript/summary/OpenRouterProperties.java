package podcastService.transcript.summary;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.Duration;

@ConfigurationProperties(prefix = "app.openrouter")
public record OpenRouterProperties(
        boolean enabled,
        String apiKey,
        String baseUrl,
        String model,
        String siteUrl,
        String appTitle,
        BigDecimal temperature,
        int maxTokens,
        Duration connectTimeout,
        Duration readTimeout,
        int maxAttempts,
        Duration retryBackoff
) {
    public OpenRouterProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://openrouter.ai/api/v1";
        }
        if (model == null || model.isBlank()) {
            model = "openrouter/free";
        }
        if (siteUrl == null || siteUrl.isBlank()) {
            siteUrl = "https://example.local";
        }
        if (appTitle == null || appTitle.isBlank()) {
            appTitle = "Podcast Summary Bot";
        }
        if (temperature == null) {
            temperature = BigDecimal.valueOf(0.3);
        }
        if (maxTokens <= 0) {
            maxTokens = 500;
        }
        if (connectTimeout == null) {
            connectTimeout = Duration.ofSeconds(3);
        }
        if (readTimeout == null) {
            readTimeout = Duration.ofSeconds(60);
        }
        if (maxAttempts <= 0) {
            maxAttempts = 3;
        }
        if (retryBackoff == null) {
            retryBackoff = Duration.ofMillis(500);
        }
    }
}
