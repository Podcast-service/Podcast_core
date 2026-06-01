package podcastService.transcript.summary;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.subtitle-storage")
public record SubtitleStorageProperties(
        String baseUrl,
        Duration connectTimeout,
        Duration readTimeout
) {
    public SubtitleStorageProperties {
        if (connectTimeout == null) {
            connectTimeout = Duration.ofSeconds(3);
        }
        if (readTimeout == null) {
            readTimeout = Duration.ofSeconds(20);
        }
    }
}
