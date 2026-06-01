package podcastService.infrastructure.outbox;

import org.junit.jupiter.api.Test;
import podcastService.infrastructure.messaging.config.KafkaMessagingProperties;
import podcastService.infrastructure.outbox.recommendation.RecommendationEventTypes;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboxEventTopicRouterTest {

    @Test
    void routesActivityEventsToActivityTopic() {
        OutboxEventTopicRouter router = router();

        assertThat(router.topicFor(RecommendationEventTypes.PODCAST_PLAY_FINISHED)).isEqualTo("activity-topic");
        assertThat(router.topicFor(RecommendationEventTypes.PODCAST_LIKED)).isEqualTo("activity-topic");
        assertThat(router.topicFor(RecommendationEventTypes.PODCAST_DISLIKED)).isEqualTo("activity-topic");
        assertThat(router.topicFor(RecommendationEventTypes.AUTHOR_FOLLOWED)).isEqualTo("activity-topic");
        assertThat(router.topicFor(RecommendationEventTypes.AUTHOR_UNFOLLOWED)).isEqualTo("activity-topic");
    }

    @Test
    void routesContentEventsToContentTopic() {
        OutboxEventTopicRouter router = router();

        assertThat(router.topicFor(RecommendationEventTypes.PODCAST_PUBLISHED)).isEqualTo("content-topic");
        assertThat(router.topicFor(RecommendationEventTypes.PODCAST_UPDATED)).isEqualTo("content-topic");
        assertThat(router.topicFor(RecommendationEventTypes.PODCAST_DELETED)).isEqualTo("content-topic");
        assertThat(router.topicFor(RecommendationEventTypes.PLAYLIST_CREATED)).isEqualTo("content-topic");
        assertThat(router.topicFor(RecommendationEventTypes.PLAYLIST_UPDATED)).isEqualTo("content-topic");
        assertThat(router.topicFor(RecommendationEventTypes.PLAYLIST_DELETED)).isEqualTo("content-topic");
    }

    @Test
    void routesFutureSearchEventsToSearchTopic() {
        OutboxEventTopicRouter router = router();

        assertThat(router.topicFor("podcast.search.reindexed.v1")).isEqualTo("search-topic");
    }

    @Test
    void rejectsUnsupportedEventTypes() {
        OutboxEventTopicRouter router = router();

        assertThatThrownBy(() -> router.topicFor("podcast.unknown.v1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported outbox event type");
    }

    private OutboxEventTopicRouter router() {
        KafkaMessagingProperties properties = new KafkaMessagingProperties();
        HashMap<String, String> topics = new HashMap<>();
        topics.put("podcast-activity-events", "activity-topic");
        topics.put("podcast-content-events", "content-topic");
        topics.put("podcast-search-events", "search-topic");
        properties.setTopics(topics);
        return new OutboxEventTopicRouter(properties);
    }
}
