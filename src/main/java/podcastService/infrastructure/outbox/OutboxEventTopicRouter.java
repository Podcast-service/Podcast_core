package podcastService.infrastructure.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import podcastService.infrastructure.messaging.config.KafkaMessagingProperties;
import podcastService.infrastructure.outbox.recommendation.RecommendationEventTypes;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class OutboxEventTopicRouter {

    private static final String ACTIVITY_TOPIC_KEY = "podcast-activity-events";
    private static final String CONTENT_TOPIC_KEY = "podcast-content-events";
    private static final String SEARCH_TOPIC_KEY = "podcast-search-events";
    private static final String SEARCH_EVENT_PREFIX = "podcast.search.";
    private static final String DEFAULT_ACTIVITY_TOPIC = "podcast.activity.events.v1";
    private static final String DEFAULT_CONTENT_TOPIC = "podcast.content.events.v1";
    private static final String DEFAULT_SEARCH_TOPIC = "podcast.search.events.v1";

    private final KafkaMessagingProperties kafkaMessagingProperties;

    public String topicFor(String eventType) {
        return switch (eventType) {
            case RecommendationEventTypes.PODCAST_PLAY_FINISHED,
                 RecommendationEventTypes.PODCAST_LIKED,
                 RecommendationEventTypes.PODCAST_DISLIKED,
                 RecommendationEventTypes.AUTHOR_FOLLOWED,
                 RecommendationEventTypes.AUTHOR_UNFOLLOWED -> topic(ACTIVITY_TOPIC_KEY, DEFAULT_ACTIVITY_TOPIC);
            case RecommendationEventTypes.PODCAST_PUBLISHED,
                 RecommendationEventTypes.PODCAST_UPDATED,
                 RecommendationEventTypes.PODCAST_DELETED,
                 RecommendationEventTypes.PLAYLIST_CREATED,
                 RecommendationEventTypes.PLAYLIST_UPDATED,
                 RecommendationEventTypes.PLAYLIST_DELETED -> topic(CONTENT_TOPIC_KEY, DEFAULT_CONTENT_TOPIC);
            default -> searchTopicForFutureEvent(eventType);
        };
    }

    private String searchTopicForFutureEvent(String eventType) {
        if (eventType != null && eventType.startsWith(SEARCH_EVENT_PREFIX) && eventType.endsWith(".v1")) {
            return topic(SEARCH_TOPIC_KEY, DEFAULT_SEARCH_TOPIC);
        }
        throw new IllegalArgumentException("Unsupported outbox event type: " + eventType);
    }

    private String topic(String topicKey, String defaultTopic) {
        Map<String, String> topics = kafkaMessagingProperties.getTopics();
        String configured = topics == null ? null : topics.get(topicKey);
        if (configured == null || configured.isBlank()) {
            return defaultTopic;
        }
        return configured.trim();
    }
}
