package podcastService.transcript.summary;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SubtitleObjectClientTest {

    @Test
    void absoluteHttpUrlIsFetchedWithoutConfiguredBaseUrl() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SubtitleObjectClient client = new SubtitleObjectClient(
                builder.build(),
                new SubtitleStorageProperties("", Duration.ofSeconds(1), Duration.ofSeconds(1))
        );

        server.expect(requestTo("https://storage.example/media/podcast/subtitles.vtt"))
                .andRespond(withSuccess("WEBVTT\n", MediaType.TEXT_PLAIN));

        assertThat(client.fetch("https://storage.example/media/podcast/subtitles.vtt"))
                .isEqualTo("WEBVTT\n");
        server.verify();
    }

    @Test
    void relativeObjectKeyIsFetchedFromConfiguredBaseUrl() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://storage.example");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SubtitleObjectClient client = new SubtitleObjectClient(
                builder.build(),
                new SubtitleStorageProperties(
                        "https://storage.example",
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(1)
                )
        );

        server.expect(requestTo("https://storage.example/media/podcast/subtitles.vtt"))
                .andRespond(withSuccess("WEBVTT\n", MediaType.TEXT_PLAIN));

        assertThat(client.fetch("media/podcast/subtitles.vtt")).isEqualTo("WEBVTT\n");
        server.verify();
    }
}
