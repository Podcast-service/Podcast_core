package podcastService.transcript.summary;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.summary")
public record SummaryGenerationProperties(
        int directMaxChars,
        int chunkSizeChars
) {
    public SummaryGenerationProperties {
        if (directMaxChars <= 0) {
            directMaxChars = 50_000;
        }
        if (chunkSizeChars <= 0) {
            chunkSizeChars = 30_000;
        }
    }
}
