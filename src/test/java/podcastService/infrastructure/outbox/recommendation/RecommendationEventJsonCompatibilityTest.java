package podcastService.infrastructure.outbox.recommendation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import podcastService.infrastructure.config.JacksonConfig;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationEventJsonCompatibilityTest {

    private final ObjectMapper objectMapper = new JacksonConfig().objectMapper();

    @ParameterizedTest
    @MethodSource("snapshots")
    void coreJsonSnapshotsContainRecommendationServiceV1Contract(
            String resource,
            String eventType,
            List<String> requiredPayloadFields
    ) throws IOException {
        JsonNode envelope = read(resource);

        assertThat(envelope.get("eventId").asText()).isNotBlank();
        assertThat(envelope.get("eventType").asText()).isEqualTo(eventType);
        assertThat(envelope.get("eventVersion").asInt()).isEqualTo(1);
        assertThat(envelope.get("producer").asText()).isEqualTo("podcast-core");
        assertThat(envelope.get("occurredAt").asText()).endsWith("Z");
        assertThat(envelope.get("payload").fieldNames()).toIterable().containsAll(requiredPayloadFields);
    }

    private JsonNode read(String resource) throws IOException {
        try (InputStream input = getClass().getResourceAsStream("/recommendation-events/" + resource)) {
            assertThat(input).as(resource).isNotNull();
            return objectMapper.readTree(input);
        }
    }

    private static Stream<Arguments> snapshots() {
        return Stream.of(
                Arguments.of(
                        "podcast.published.v1.json",
                        RecommendationEventTypes.PODCAST_PUBLISHED,
                        List.of("podcastId", "authorId", "categoryId", "title", "publishedAt")
                ),
                Arguments.of(
                        "podcast.play_finished.v1.json",
                        RecommendationEventTypes.PODCAST_PLAY_FINISHED,
                        List.of("podcastId", "userId", "progressSeconds", "finishedAt", "authorId", "categoryId", "progressPercent")
                ),
                Arguments.of(
                        "podcast.liked.v1.json",
                        RecommendationEventTypes.PODCAST_LIKED,
                        List.of("podcastId", "userId", "likedAt", "authorId", "categoryId")
                ),
                Arguments.of(
                        "playlist.updated.v1.json",
                        RecommendationEventTypes.PLAYLIST_UPDATED,
                        List.of("playlistId", "ownerUserId", "title", "publicPlaylist", "updatedAt", "podcastIds")
                )
        );
    }
}
