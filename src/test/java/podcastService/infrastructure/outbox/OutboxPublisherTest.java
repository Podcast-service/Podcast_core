package podcastService.infrastructure.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.support.SendResult;
import podcastService.infrastructure.config.JacksonConfig;
import podcastService.infrastructure.messaging.config.KafkaMessagingProperties;
import podcastService.infrastructure.outbox.entity.OutboxEventEntity;
import podcastService.infrastructure.outbox.recommendation.RecommendationEventTypes;
import podcastService.infrastructure.outbox.repository.OutboxEventRepository;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxPublisherTest {

    private static final UUID EVENT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final String EVENT_KEY = "20000000-0000-0000-0000-000000000001";

    private final ObjectMapper objectMapper = new JacksonConfig().objectMapper();

    private OutboxEventRepository repository;
    private KafkaOperations<Object, Object> kafkaOperations;
    private KafkaMessagingProperties kafkaMessagingProperties;

    @BeforeEach
    void setUp() {
        repository = mock(OutboxEventRepository.class);
        kafkaOperations = mock(KafkaOperations.class);
        kafkaMessagingProperties = kafkaProperties(true);
    }

    @Test
    void successFlowPublishesNewEventAndMarksSent() {
        OutboxEventEntity event = event(OutboxEventStatus.NEW, RecommendationEventTypes.PODCAST_LIKED, 0);
        when(repository.lockNextPublishBatch(100, 10)).thenReturn(List.of(event));
        CompletableFuture<SendResult<Object, Object>> published = CompletableFuture.completedFuture(null);
        when(kafkaOperations.send(anyString(), anyString(), anyString())).thenReturn(published);

        int sent = publisher(true, 100, 10).publishBatch();

        assertThat(sent).isEqualTo(1);
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.SENT);
        assertThat(event.getSentAt()).isNotNull();
        assertThat(event.getLastError()).isNull();
        verify(kafkaOperations).send(
                eq("podcast.activity.events.v1"),
                eq(EVENT_KEY),
                eq(event.getPayload().toString())
        );
        verify(repository, atLeastOnce()).saveAndFlush(event);
    }

    @ParameterizedTest
    @EnumSource(value = OutboxEventStatus.class, names = {"NEW", "PROCESSING"})
    void failureFlowMarksEventFailedAndIncrementsRetryCount(OutboxEventStatus initialStatus) {
        OutboxEventEntity event = event(initialStatus, RecommendationEventTypes.PODCAST_PUBLISHED, 3);
        when(repository.lockNextPublishBatch(100, 10)).thenReturn(List.of(event));
        CompletableFuture<SendResult<Object, Object>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("broker down"));
        when(kafkaOperations.send(anyString(), anyString(), anyString())).thenReturn(failed);

        int sent = publisher(true, 100, 10).publishBatch();

        assertThat(sent).isZero();
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
        assertThat(event.getRetryCount()).isEqualTo(4);
        assertThat(event.getLastError()).contains("broker down");
        assertThat(event.getAvailableAt()).isAfter(OffsetDateTime.now().minusSeconds(1));
        assertThat(event.getSentAt()).isNull();
        verify(kafkaOperations).send(eq("podcast.content.events.v1"), eq(EVENT_KEY), eq(event.getPayload().toString()));
    }

    @Test
    void retryLimitIsRespectedByRepositoryBatchQuery() {
        when(repository.lockNextPublishBatch(100, 10)).thenReturn(List.of());

        int sent = publisher(true, 100, 10).publishBatch();

        assertThat(sent).isZero();
        verify(repository).lockNextPublishBatch(100, 10);
        verify(kafkaOperations, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void publisherDisabledDoesNotReadOrSendEvents() {
        int sent = publisher(false, 100, 10).publishBatch();

        assertThat(sent).isZero();
        verify(repository, never()).lockNextPublishBatch(anyInt(), anyInt());
        verify(kafkaOperations, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void kafkaProducerDisabledDoesNotReadOrSendEvents() {
        kafkaMessagingProperties = kafkaProperties(false);

        int sent = publisher(true, 100, 10).publishBatch();

        assertThat(sent).isZero();
        verify(repository, never()).lockNextPublishBatch(anyInt(), anyInt());
        verify(kafkaOperations, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void eventKeyIsUsedAsKafkaMessageKey() {
        OutboxEventEntity event = event(OutboxEventStatus.NEW, RecommendationEventTypes.PLAYLIST_UPDATED, 0);
        event.setEventKey("playlist-key");
        when(repository.lockNextPublishBatch(1, 2)).thenReturn(List.of(event));
        CompletableFuture<SendResult<Object, Object>> published = CompletableFuture.completedFuture(null);
        when(kafkaOperations.send(anyString(), anyString(), anyString())).thenReturn(published);

        publisher(true, 1, 2).publishBatch();

        verify(kafkaOperations).send(eq("podcast.content.events.v1"), eq("playlist-key"), eq(event.getPayload().toString()));
    }

    private OutboxPublisher publisher(boolean enabled, int batchSize, int maxRetryAttempts) {
        return new OutboxPublisher(
                repository,
                kafkaOperations,
                kafkaMessagingProperties,
                new OutboxPublisherProperties(enabled, batchSize, 3_000, maxRetryAttempts),
                new OutboxEventTopicRouter(kafkaMessagingProperties),
                new SimpleMeterRegistry()
        );
    }

    private KafkaMessagingProperties kafkaProperties(boolean producerEnabled) {
        KafkaMessagingProperties properties = new KafkaMessagingProperties();
        HashMap<String, String> topics = new HashMap<>();
        topics.put("podcast-activity-events", "podcast.activity.events.v1");
        topics.put("podcast-content-events", "podcast.content.events.v1");
        topics.put("podcast-search-events", "podcast.search.events.v1");
        properties.setTopics(topics);
        properties.getProducer().setEnabled(producerEnabled);
        return properties;
    }

    private OutboxEventEntity event(OutboxEventStatus status, String eventType, int retryCount) {
        OutboxEventEntity event = new OutboxEventEntity();
        event.setId(EVENT_ID);
        event.setAggregateType("USER_ACTIVITY");
        event.setAggregateId(UUID.fromString(EVENT_KEY));
        event.setEventType(eventType);
        event.setEventVersion(1);
        event.setEventKey(EVENT_KEY);
        event.setPayload(payload(eventType));
        event.setStatus(status);
        event.setRetryCount(retryCount);
        return event;
    }

    private JsonNode payload(String eventType) {
        return objectMapper.createObjectNode()
                .put("eventId", EVENT_ID.toString())
                .put("eventType", eventType)
                .put("eventVersion", 1)
                .put("producer", "podcast-core");
    }
}
