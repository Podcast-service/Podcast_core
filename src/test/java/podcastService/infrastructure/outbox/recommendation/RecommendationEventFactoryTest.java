package podcastService.infrastructure.outbox.recommendation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import podcastService.infrastructure.config.JacksonConfig;
import podcastService.infrastructure.outbox.DomainEventEnvelope;
import podcastService.infrastructure.outbox.recommendation.payload.AuthorFollowedPayload;
import podcastService.infrastructure.outbox.recommendation.payload.AuthorUnfollowedPayload;
import podcastService.infrastructure.outbox.recommendation.payload.PlaylistCreatedPayload;
import podcastService.infrastructure.outbox.recommendation.payload.PlaylistDeletedPayload;
import podcastService.infrastructure.outbox.recommendation.payload.PlaylistUpdatedPayload;
import podcastService.infrastructure.outbox.recommendation.payload.PodcastDeletedPayload;
import podcastService.infrastructure.outbox.recommendation.payload.PodcastDislikedPayload;
import podcastService.infrastructure.outbox.recommendation.payload.PodcastLikedPayload;
import podcastService.infrastructure.outbox.recommendation.payload.PodcastPlayFinishedPayload;
import podcastService.infrastructure.outbox.recommendation.payload.PodcastPublishedPayload;
import podcastService.infrastructure.outbox.recommendation.payload.PodcastUpdatedPayload;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationEventFactoryTest {

    private static final UUID PODCAST_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID AUTHOR_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID CATEGORY_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID PLAYLIST_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final Instant OCCURRED_AT = Instant.parse("2026-06-01T10:15:30Z");

    private final ObjectMapper objectMapper = new JacksonConfig().objectMapper();

    @Test
    void podcastContentFactoryCreatesVersionedEvents() {
        DomainEventEnvelope published = podcastPublished();
        DomainEventEnvelope updated = podcastUpdated();
        DomainEventEnvelope deleted = podcastDeleted();

        assertEnvelope(published, RecommendationEventTypes.PODCAST_PUBLISHED, USER_ID, PodcastPublishedPayload.class);
        assertEnvelope(updated, RecommendationEventTypes.PODCAST_UPDATED, USER_ID, PodcastUpdatedPayload.class);
        assertEnvelope(deleted, RecommendationEventTypes.PODCAST_DELETED, USER_ID, PodcastDeletedPayload.class);
    }

    @Test
    void podcastActivityFactoryCreatesVersionedEvents() {
        DomainEventEnvelope playFinished = podcastPlayFinished();
        DomainEventEnvelope liked = podcastLiked();
        DomainEventEnvelope disliked = podcastDisliked();

        assertEnvelope(playFinished, RecommendationEventTypes.PODCAST_PLAY_FINISHED, USER_ID, PodcastPlayFinishedPayload.class);
        assertEnvelope(liked, RecommendationEventTypes.PODCAST_LIKED, USER_ID, PodcastLikedPayload.class);
        assertEnvelope(disliked, RecommendationEventTypes.PODCAST_DISLIKED, USER_ID, PodcastDislikedPayload.class);
    }

    @Test
    void authorActivityFactoryCreatesVersionedEvents() {
        DomainEventEnvelope followed = authorFollowed();
        DomainEventEnvelope unfollowed = authorUnfollowed();

        assertEnvelope(followed, RecommendationEventTypes.AUTHOR_FOLLOWED, USER_ID, AuthorFollowedPayload.class);
        assertEnvelope(unfollowed, RecommendationEventTypes.AUTHOR_UNFOLLOWED, USER_ID, AuthorUnfollowedPayload.class);
    }

    @Test
    void playlistContentFactoryCreatesVersionedEvents() {
        DomainEventEnvelope created = playlistCreated();
        DomainEventEnvelope updated = playlistUpdated();
        DomainEventEnvelope deleted = playlistDeleted();

        assertEnvelope(created, RecommendationEventTypes.PLAYLIST_CREATED, USER_ID, PlaylistCreatedPayload.class);
        assertEnvelope(updated, RecommendationEventTypes.PLAYLIST_UPDATED, USER_ID, PlaylistUpdatedPayload.class);
        assertEnvelope(deleted, RecommendationEventTypes.PLAYLIST_DELETED, USER_ID, PlaylistDeletedPayload.class);
    }

    @ParameterizedTest
    @MethodSource("events")
    void recommendationEventsHaveStableJsonStructure(DomainEventEnvelope envelope, String expectedEventType, String payloadIdField) {
        JsonNode json = objectMapper.valueToTree(envelope);

        assertThat(json.get("eventId").asText()).isNotBlank();
        assertThat(json.get("eventType").asText()).isEqualTo(expectedEventType);
        assertThat(json.get("eventType").asText()).endsWith(".v1");
        assertThat(json.get("eventVersion").asInt()).isEqualTo(1);
        assertThat(json.get("producer").asText()).isEqualTo("podcast-core");
        assertThat(json.get("occurredAt")).isNotNull();
        assertThat(json.get("correlationId").asText()).isEqualTo("correlation-1");
        assertThat(json.get("causationId").asText()).isEqualTo("causation-1");
        assertThat(json.get("userId").asText()).isEqualTo(USER_ID.toString());
        assertThat(json.get("payload").get(payloadIdField).asText()).isNotBlank();
    }

    private void assertEnvelope(
            DomainEventEnvelope envelope,
            String expectedEventType,
            UUID expectedUserId,
            Class<?> expectedPayloadType
    ) {
        assertThat(envelope.eventId()).isNotNull();
        assertThat(envelope.eventType()).isEqualTo(expectedEventType);
        assertThat(envelope.eventType()).endsWith(".v1");
        assertThat(envelope.eventVersion()).isEqualTo(1);
        assertThat(envelope.producer()).isEqualTo("podcast-core");
        assertThat(envelope.occurredAt()).isEqualTo(OCCURRED_AT);
        assertThat(envelope.correlationId()).isEqualTo("correlation-1");
        assertThat(envelope.causationId()).isEqualTo("causation-1");
        assertThat(envelope.userId()).isEqualTo(expectedUserId);
        assertThat(envelope.payload()).isInstanceOf(expectedPayloadType);
    }

    private static Stream<Arguments> events() {
        return Stream.of(
                Arguments.of(podcastPublished(), RecommendationEventTypes.PODCAST_PUBLISHED, "podcastId"),
                Arguments.of(podcastUpdated(), RecommendationEventTypes.PODCAST_UPDATED, "podcastId"),
                Arguments.of(podcastDeleted(), RecommendationEventTypes.PODCAST_DELETED, "podcastId"),
                Arguments.of(podcastPlayFinished(), RecommendationEventTypes.PODCAST_PLAY_FINISHED, "podcastId"),
                Arguments.of(podcastLiked(), RecommendationEventTypes.PODCAST_LIKED, "podcastId"),
                Arguments.of(podcastDisliked(), RecommendationEventTypes.PODCAST_DISLIKED, "podcastId"),
                Arguments.of(authorFollowed(), RecommendationEventTypes.AUTHOR_FOLLOWED, "authorId"),
                Arguments.of(authorUnfollowed(), RecommendationEventTypes.AUTHOR_UNFOLLOWED, "authorId"),
                Arguments.of(playlistCreated(), RecommendationEventTypes.PLAYLIST_CREATED, "playlistId"),
                Arguments.of(playlistUpdated(), RecommendationEventTypes.PLAYLIST_UPDATED, "playlistId"),
                Arguments.of(playlistDeleted(), RecommendationEventTypes.PLAYLIST_DELETED, "playlistId")
        );
    }

    private static DomainEventEnvelope podcastPublished() {
        return PodcastContentEventFactory.published(
                PODCAST_ID,
                AUTHOR_ID,
                CATEGORY_ID,
                "Podcast title",
                OCCURRED_AT,
                USER_ID,
                "correlation-1",
                "causation-1"
        );
    }

    private static DomainEventEnvelope podcastUpdated() {
        return PodcastContentEventFactory.updated(
                PODCAST_ID,
                AUTHOR_ID,
                CATEGORY_ID,
                "Updated podcast title",
                OCCURRED_AT,
                USER_ID,
                "correlation-1",
                "causation-1"
        );
    }

    private static DomainEventEnvelope podcastDeleted() {
        return PodcastContentEventFactory.deleted(
                PODCAST_ID,
                AUTHOR_ID,
                OCCURRED_AT,
                USER_ID,
                "correlation-1",
                "causation-1"
        );
    }

    private static DomainEventEnvelope podcastPlayFinished() {
        return PodcastActivityEventFactory.playFinished(
                PODCAST_ID,
                USER_ID,
                1800,
                OCCURRED_AT,
                "correlation-1",
                "causation-1"
        );
    }

    private static DomainEventEnvelope podcastLiked() {
        return PodcastActivityEventFactory.liked(
                PODCAST_ID,
                USER_ID,
                OCCURRED_AT,
                "correlation-1",
                "causation-1"
        );
    }

    private static DomainEventEnvelope podcastDisliked() {
        return PodcastActivityEventFactory.disliked(
                PODCAST_ID,
                USER_ID,
                OCCURRED_AT,
                "correlation-1",
                "causation-1"
        );
    }

    private static DomainEventEnvelope authorFollowed() {
        return AuthorActivityEventFactory.followed(
                AUTHOR_ID,
                USER_ID,
                OCCURRED_AT,
                "correlation-1",
                "causation-1"
        );
    }

    private static DomainEventEnvelope authorUnfollowed() {
        return AuthorActivityEventFactory.unfollowed(
                AUTHOR_ID,
                USER_ID,
                OCCURRED_AT,
                "correlation-1",
                "causation-1"
        );
    }

    private static DomainEventEnvelope playlistCreated() {
        return PlaylistContentEventFactory.created(
                PLAYLIST_ID,
                USER_ID,
                "Playlist title",
                true,
                OCCURRED_AT,
                "correlation-1",
                "causation-1"
        );
    }

    private static DomainEventEnvelope playlistUpdated() {
        return PlaylistContentEventFactory.updated(
                PLAYLIST_ID,
                USER_ID,
                "Updated playlist title",
                false,
                OCCURRED_AT,
                "correlation-1",
                "causation-1"
        );
    }

    private static DomainEventEnvelope playlistDeleted() {
        return PlaylistContentEventFactory.deleted(
                PLAYLIST_ID,
                USER_ID,
                OCCURRED_AT,
                "correlation-1",
                "causation-1"
        );
    }
}
