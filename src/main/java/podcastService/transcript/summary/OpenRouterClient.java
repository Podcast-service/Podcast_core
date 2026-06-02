package podcastService.transcript.summary;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Slf4j
@Component
public class OpenRouterClient {

    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";

    private final RestClient openRouterRestClient;
    private final OpenRouterProperties properties;

    public OpenRouterClient(
            @Qualifier("openRouterRestClient") RestClient openRouterRestClient,
            OpenRouterProperties properties
    ) {
        this.openRouterRestClient = openRouterRestClient;
        this.properties = properties;
    }

    public String complete(List<OpenRouterMessage> messages) {
        if (!properties.enabled()) {
            throw new OpenRouterClientException("OpenRouter summary generation is disabled");
        }
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new OpenRouterClientException("OpenRouter API key is not configured");
        }

        int maxAttempts = Math.max(1, properties.maxAttempts());
        RuntimeException lastException = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return send(messages);
            } catch (RestClientResponseException exception) {
                lastException = mapResponseException(exception);
                if (!isRetryable(exception) || attempt == maxAttempts) {
                    throw lastException;
                }
                log.warn("OpenRouter request failed with retryable status, status={}, attempt={}/{}",
                        exception.getStatusCode().value(), attempt, maxAttempts);
                sleepBeforeRetry();
            } catch (RestClientException exception) {
                lastException = new OpenRouterClientException("OpenRouter request failed", exception);
                if (attempt == maxAttempts) {
                    throw lastException;
                }
                log.warn("OpenRouter request failed with retryable client error, attempt={}/{}, reason={}",
                        attempt, maxAttempts, safeReason(exception));
                sleepBeforeRetry();
            }
        }
        throw lastException == null
                ? new OpenRouterClientException("OpenRouter request failed")
                : lastException;
    }

    private String send(List<OpenRouterMessage> messages) {
        OpenRouterChatRequest request = new OpenRouterChatRequest(
                properties.model(),
                messages,
                properties.temperature(),
                properties.maxTokens()
        );

        OpenRouterChatResponse response = openRouterRestClient
                .post()
                .uri(CHAT_COMPLETIONS_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey().trim())
                .header("HTTP-Referer", properties.siteUrl())
                .header("X-OpenRouter-Title", properties.appTitle())
                .body(request)
                .retrieve()
                .body(OpenRouterChatResponse.class);

        return extractContent(response);
    }

    private String extractContent(OpenRouterChatResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new OpenRouterClientException("OpenRouter returned empty choices");
        }

        OpenRouterChatResponse.Message message = response.choices().getFirst().message();
        if (message == null || message.content() == null || message.content().isBlank()) {
            throw new OpenRouterClientException("OpenRouter returned empty message content");
        }

        return message.content().trim();
    }

    private RuntimeException mapResponseException(RestClientResponseException exception) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        log.warn(
                "OpenRouter request rejected, status={}, responseLength={}",
                exception.getStatusCode().value(),
                exception.getResponseBodyAsString() == null ? 0 : exception.getResponseBodyAsString().length()
        );

        if (status == HttpStatus.UNAUTHORIZED || status == HttpStatus.FORBIDDEN) {
            return new OpenRouterClientException("OpenRouter authentication failed");
        }
        if (exception.getStatusCode().is4xxClientError() && status != HttpStatus.TOO_MANY_REQUESTS) {
            return new OpenRouterClientException("OpenRouter rejected summary request");
        }
        return new OpenRouterClientException("OpenRouter service is unavailable", exception);
    }

    private boolean isRetryable(RestClientResponseException exception) {
        return exception.getStatusCode().value() == 429 || exception.getStatusCode().is5xxServerError();
    }

    private void sleepBeforeRetry() {
        if (properties.retryBackoff().isZero() || properties.retryBackoff().isNegative()) {
            return;
        }
        try {
            Thread.sleep(properties.retryBackoff().toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new OpenRouterClientException("OpenRouter retry interrupted", exception);
        }
    }

    private String safeReason(RestClientException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message.length() > 300 ? message.substring(0, 300) : message;
    }
}
