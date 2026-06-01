package podcastService.infrastructure.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.support.SendResult;
import podcastService.infrastructure.config.JacksonConfig;
import podcastService.infrastructure.messaging.config.KafkaMessagingProperties;
import podcastService.infrastructure.outbox.entity.OutboxEventEntity;
import podcastService.infrastructure.outbox.recommendation.RecommendationEventTypes;
import podcastService.infrastructure.outbox.repository.OutboxEventRepository;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OutboxPublisherTest {

    private static final UUID EVENT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final String EVENT_KEY = "20000000-0000-0000-0000-000000000001";

    private final ObjectMapper objectMapper = new JacksonConfig().objectMapper();

    private OutboxEventRepository repository;
    private OutboxPublisherTransactionService transactionService;
    private KafkaOperations<Object, Object> kafkaOperations;
    private KafkaMessagingProperties kafkaMessagingProperties;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        repository = mock(OutboxEventRepository.class);
        transactionService = mock(OutboxPublisherTransactionService.class);
        kafkaOperations = mock(KafkaOperations.class);
        kafkaMessagingProperties = kafkaProperties(true);
        meterRegistry = new SimpleMeterRegistry();
    }

    @Test
    void successFlowClaimsPublishesAndMarksSent() {
        OutboxEventEntity event = event(RecommendationEventTypes.PODCAST_LIKED, 0);
        when(transactionService.claimNextPublishBatch(100, 10)).thenReturn(List.of(event));
        when(transactionService.markSent(EVENT_ID)).thenReturn(true);
        when(kafkaOperations.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        int sent = publisher(true, 100, 10, 10_000).publishBatch();

        assertThat(sent).isEqualTo(1);
        verify(kafkaOperations).send(
                eq("podcast.activity.events.v1"),
                eq(EVENT_KEY),
                eq(event.getPayload().toString())
        );
        var inOrder = inOrder(transactionService, kafkaOperations);
        inOrder.verify(transactionService).claimNextPublishBatch(100, 10);
        inOrder.verify(kafkaOperations).send(anyString(), anyString(), anyString());
        inOrder.verify(transactionService).markSent(EVENT_ID);
    }

    @Test
    void failureFlowMarksEventFailedAndSchedulesRetry() {
        OutboxEventEntity event = event(RecommendationEventTypes.PODCAST_PUBLISHED, 3);
        when(transactionService.claimNextPublishBatch(100, 10)).thenReturn(List.of(event));
        CompletableFuture<SendResult<Object, Object>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("broker down"));
        when(kafkaOperations.send(anyString(), anyString(), anyString())).thenReturn(failed);
        when(transactionService.markFailed(eq(EVENT_ID), contains("broker down"), eq(Duration.ofMillis(3_000))))
                .thenReturn(true);

        int sent = publisher(true, 100, 10, 10_000).publishBatch();

        assertThat(sent).isZero();
        verify(kafkaOperations).send(eq("podcast.content.events.v1"), eq(EVENT_KEY), eq(event.getPayload().toString()));
        verify(transactionService).markFailed(EVENT_ID, "java.lang.RuntimeException: broker down", Duration.ofMillis(3_000));
        assertThat(meterRegistry.counter("outbox.events.failed").count()).isEqualTo(1.0);
        assertThat(meterRegistry.counter("outbox.events.retry").count()).isEqualTo(1.0);
    }

    @Test
    void sendTimeoutMarksEventFailedInsteadOfBlockingForever() {
        OutboxEventEntity event = event(RecommendationEventTypes.PODCAST_LIKED, 0);
        when(transactionService.claimNextPublishBatch(100, 10)).thenReturn(List.of(event));
        when(kafkaOperations.send(anyString(), anyString(), anyString())).thenReturn(new CompletableFuture<>());
        when(transactionService.markFailed(eq(EVENT_ID), eq("TimeoutException"), eq(Duration.ofMillis(3_000))))
                .thenReturn(true);

        int sent = publisher(true, 100, 10, 1).publishBatch();

        assertThat(sent).isZero();
        verify(transactionService).markFailed(EVENT_ID, "TimeoutException", Duration.ofMillis(3_000));
    }

    @Test
    void retryLimitIsRespectedByClaimTransaction() {
        when(transactionService.claimNextPublishBatch(100, 10)).thenReturn(List.of());

        int sent = publisher(true, 100, 10, 10_000).publishBatch();

        assertThat(sent).isZero();
        verify(transactionService).claimNextPublishBatch(100, 10);
        verify(kafkaOperations, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void publisherDisabledDoesNotRecoverReadOrSendEvents() {
        int sent = publisher(false, 100, 10, 10_000).publishBatch();

        assertThat(sent).isZero();
        verifyNoInteractions(transactionService);
        verify(kafkaOperations, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void kafkaProducerDisabledRecoversStaleEventsButDoesNotClaimOrSend() {
        kafkaMessagingProperties = kafkaProperties(false);

        int sent = publisher(true, 100, 10, 10_000).publishBatch();

        assertThat(sent).isZero();
        verify(transactionService).recoverStaleProcessing(Duration.ofMillis(600_000));
        verify(transactionService, never()).claimNextPublishBatch(anyInt(), anyInt());
        verify(kafkaOperations, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void staleProcessingRecoveryIncrementsMetric() {
        when(transactionService.recoverStaleProcessing(Duration.ofMillis(600_000))).thenReturn(2);
        when(transactionService.claimNextPublishBatch(100, 10)).thenReturn(List.of());

        publisher(true, 100, 10, 10_000).publishBatch();

        assertThat(meterRegistry.counter("outbox.events.processing.recovered").count()).isEqualTo(2.0);
    }

    @Test
    void eventKeyIsUsedAsKafkaMessageKey() {
        OutboxEventEntity event = event(RecommendationEventTypes.PLAYLIST_UPDATED, 0);
        event.setEventKey("playlist-key");
        when(transactionService.claimNextPublishBatch(1, 2)).thenReturn(List.of(event));
        when(transactionService.markSent(EVENT_ID)).thenReturn(true);
        when(kafkaOperations.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher(true, 1, 2, 10_000).publishBatch();

        verify(kafkaOperations).send(eq("podcast.content.events.v1"), eq("playlist-key"), eq(event.getPayload().toString()));
    }

    private OutboxPublisher publisher(boolean enabled, int batchSize, int maxRetryAttempts, long sendTimeoutMs) {
        return new OutboxPublisher(
                repository,
                transactionService,
                kafkaOperations,
                kafkaMessagingProperties,
                new OutboxPublisherProperties(enabled, batchSize, 3_000, maxRetryAttempts, 600_000, sendTimeoutMs),
                new OutboxEventTopicRouter(kafkaMessagingProperties),
                meterRegistry
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

    private OutboxEventEntity event(String eventType, int retryCount) {
        OutboxEventEntity event = new OutboxEventEntity();
        event.setId(EVENT_ID);
        event.setAggregateType("USER_ACTIVITY");
        event.setAggregateId(UUID.fromString(EVENT_KEY));
        event.setEventType(eventType);
        event.setEventVersion(1);
        event.setEventKey(EVENT_KEY);
        event.setPayload(payload(eventType));
        event.setStatus(OutboxEventStatus.PROCESSING);
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
