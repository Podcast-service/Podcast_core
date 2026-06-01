package podcastService.infrastructure.outbox.recommendation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import podcastService.infrastructure.outbox.DomainEventEnvelope;
import podcastService.infrastructure.outbox.OutboxEventService;
import podcastService.infrastructure.outbox.RecommendationEventsProperties;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecommendationOutboxEventService {

    private static final String AGGREGATE_PODCAST = "PODCAST";
    private static final String AGGREGATE_AUTHOR = "AUTHOR";
    private static final String AGGREGATE_PLAYLIST = "PLAYLIST";
    private static final String AGGREGATE_USER_ACTIVITY = "USER_ACTIVITY";

    private final OutboxEventService outboxEventService;
    private final RecommendationEventsProperties properties;

    public void savePodcastContentEvent(UUID podcastId, DomainEventEnvelope envelope) {
        save(AGGREGATE_PODCAST, podcastId, podcastId, envelope);
    }

    public void saveAuthorEvent(UUID authorId, DomainEventEnvelope envelope) {
        save(AGGREGATE_AUTHOR, authorId, authorId, envelope);
    }

    public void savePlaylistEvent(UUID playlistId, DomainEventEnvelope envelope) {
        save(AGGREGATE_PLAYLIST, playlistId, playlistId, envelope);
    }

    public void saveUserActivityEvent(UUID userId, DomainEventEnvelope envelope) {
        save(AGGREGATE_USER_ACTIVITY, userId, userId, envelope);
    }

    private void save(String aggregateType, UUID aggregateId, UUID eventKey, DomainEventEnvelope envelope) {
        if (!properties.enabled()) {
            return;
        }

        outboxEventService.saveEvent(
                aggregateType,
                aggregateId,
                eventKey == null ? null : eventKey.toString(),
                envelope
        );
    }
}
