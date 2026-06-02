package podcastService.transcript.summary;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenRouterClientTest {

    private static final String BASE_URL = "https://openrouter.example/api/v1";

    @Test
    void successfulResponseIsParsed() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenRouterClient client = client(builder.build(), 1);

        server.expect(requestTo(BASE_URL + "/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer fake-key"))
                .andRespond(withSuccess("""
                        {"choices":[{"message":{"content":" Готовое summary "}}]}
                        """, MediaType.APPLICATION_JSON));

        String result = client.complete(messages());

        assertThat(result).isEqualTo("Готовое summary");
        server.verify();
    }

    @Test
    void emptyChoicesThrowsException() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenRouterClient client = client(builder.build(), 1);

        server.expect(requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withSuccess("{\"choices\":[]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.complete(messages()))
                .isInstanceOf(OpenRouterClientException.class)
                .hasMessage("OpenRouter returned empty choices");
    }

    @Test
    void emptyMessageContentThrowsException() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenRouterClient client = client(builder.build(), 1);

        server.expect(requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withSuccess("{\"choices\":[{\"message\":{\"content\":\"  \"}}]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.complete(messages()))
                .isInstanceOf(OpenRouterClientException.class)
                .hasMessage("OpenRouter returned empty message content");
    }

    @Test
    void non2xxResponseIsHandled() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenRouterClient client = client(builder.build(), 1);

        server.expect(requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.complete(messages()))
                .isInstanceOf(OpenRouterClientException.class)
                .hasMessage("OpenRouter service is unavailable");
    }

    @Test
    void unauthorizedResponseIsNotRetried() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenRouterClient client = client(builder.build(), 3);

        server.expect(ExpectedCount.once(), requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.complete(messages()))
                .isInstanceOf(OpenRouterClientException.class)
                .hasMessage("OpenRouter authentication failed");
        server.verify();
    }

    @Test
    void tooManyRequestsResponseIsRetried() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenRouterClient client = client(builder.build(), 2);

        server.expect(ExpectedCount.once(), requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS));
        server.expect(ExpectedCount.once(), requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withSuccess("""
                        {"choices":[{"message":{"content":"retry success"}}]}
                        """, MediaType.APPLICATION_JSON));

        assertThat(client.complete(messages())).isEqualTo("retry success");
        server.verify();
    }

    private OpenRouterClient client(RestClient restClient, int maxAttempts) {
        return new OpenRouterClient(
                restClient,
                new OpenRouterProperties(
                        true,
                        "fake-key",
                        BASE_URL,
                        "openrouter/free",
                        "https://example.test",
                        "Podcast Summary Bot",
                        BigDecimal.valueOf(0.3),
                        500,
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(1),
                        maxAttempts,
                        Duration.ZERO
                )
        );
    }

    private List<OpenRouterMessage> messages() {
        return List.of(
                new OpenRouterMessage("system", "system"),
                new OpenRouterMessage("user", "user")
        );
    }
}
