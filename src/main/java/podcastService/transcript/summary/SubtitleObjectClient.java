package podcastService.transcript.summary;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;

@Slf4j
@Component
public class SubtitleObjectClient {

    private final RestClient subtitleStorageRestClient;
    private final SubtitleStorageProperties properties;

    public SubtitleObjectClient(
            @Qualifier("subtitleStorageRestClient") RestClient subtitleStorageRestClient,
            SubtitleStorageProperties properties
    ) {
        this.subtitleStorageRestClient = subtitleStorageRestClient;
        this.properties = properties;
    }

    public String fetch(String objectKey) {
        String normalizedKey = normalizeObjectKey(objectKey);
        try {
            RestClient.RequestHeadersSpec<?> request;
            if (isAbsoluteUrl(normalizedKey)) {
                request = subtitleStorageRestClient.get().uri(URI.create(normalizedKey));
            } else {
                if (properties.baseUrl() == null || properties.baseUrl().isBlank()) {
                    throw new SubtitleObjectStorageException("Subtitle storage base URL is not configured");
                }
                request = subtitleStorageRestClient.get().uri(uriBuilder -> {
                    for (String segment : normalizedKey.split("/")) {
                        uriBuilder.pathSegment(segment);
                    }
                    return uriBuilder.build();
                });
            }
            String content = request
                    .retrieve()
                    .body(String.class);
            if (content == null || content.isBlank()) {
                throw new SubtitleObjectStorageException("Subtitle object is empty");
            }
            return content;
        } catch (RestClientException exception) {
            log.warn("Subtitle object fetch failed, objectKeyLength={}, reason={}",
                    normalizedKey.length(), safeReason(exception));
            throw new SubtitleObjectStorageException("Subtitle object storage is unavailable", exception);
        }
    }

    private String normalizeObjectKey(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new SummaryGenerationException("Subtitle object key is missing");
        }
        String normalized = objectKey.trim();
        URI uri;
        try {
            uri = URI.create(normalized);
        } catch (IllegalArgumentException exception) {
            throw new SummaryGenerationException("Subtitle object key is invalid");
        }
        if (uri.isAbsolute()) {
            if (!isAbsoluteUrl(normalized) || uri.getHost() == null) {
                throw new SummaryGenerationException("Subtitle object URL is invalid");
            }
            return normalized;
        }
        if (normalized.startsWith("/") || normalized.contains("..")) {
            throw new SummaryGenerationException("Subtitle object key is invalid");
        }
        return normalized;
    }

    private boolean isAbsoluteUrl(String value) {
        URI uri = URI.create(value);
        return "http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme());
    }

    private String safeReason(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message.length() > 300 ? message.substring(0, 300) : message;
    }
}
