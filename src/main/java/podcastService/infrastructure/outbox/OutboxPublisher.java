package podcastService.infrastructure.outbox;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import podcastService.infrastructure.messaging.config.KafkaMessagingProperties;
import podcastService.infrastructure.outbox.entity.OutboxEventEntity;
import podcastService.infrastructure.outbox.repository.OutboxEventRepository;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "app.outbox.publisher", name = "enabled", havingValue = "true")
public class OutboxPublisher {

    private static final int MAX_ERROR_LENGTH = 1_000;

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxPublisherTransactionService transactionService;
    private final KafkaOperations<Object, Object> kafkaOperations;
    private final KafkaMessagingProperties kafkaMessagingProperties;
    private final OutboxPublisherProperties properties;
    private final OutboxEventTopicRouter topicRouter;
    private final Counter sentCounter;
    private final Counter failedCounter;
    private final Counter retryCounter;
    private final Counter processingRecoveredCounter;

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            OutboxPublisherTransactionService transactionService,
            KafkaOperations<Object, Object> kafkaOperations,
            KafkaMessagingProperties kafkaMessagingProperties,
            OutboxPublisherProperties properties,
            OutboxEventTopicRouter topicRouter,
            MeterRegistry meterRegistry
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.transactionService = transactionService;
        this.kafkaOperations = kafkaOperations;
        this.kafkaMessagingProperties = kafkaMessagingProperties;
        this.properties = properties;
        this.topicRouter = topicRouter;
        this.sentCounter = Counter.builder("outbox.events.sent").register(meterRegistry);
        this.failedCounter = Counter.builder("outbox.events.failed").register(meterRegistry);
        this.retryCounter = Counter.builder("outbox.events.retry").register(meterRegistry);
        this.processingRecoveredCounter = Counter.builder("outbox.events.processing.recovered").register(meterRegistry);
        Gauge.builder("outbox.events.pending", outboxEventRepository,
                        repository -> repository.countPublishable(properties.maxRetryAttempts()))
                .register(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${app.outbox.publisher.publish-delay-ms:3000}")
    public void publishScheduledBatch() {
        publishBatch();
    }

    public int publishBatch() {
        if (!properties.enabled()) {
            log.debug("Outbox publisher disabled; skipping batch");
            return 0;
        }

        recoverStaleProcessing();

        if (!kafkaMessagingProperties.getProducer().isEnabled()) {
            log.debug("Kafka producer disabled; skipping outbox batch");
            return 0;
        }

        List<OutboxEventEntity> events = transactionService.claimNextPublishBatch(
                properties.batchSize(),
                properties.maxRetryAttempts()
        );
        if (events.isEmpty()) {
            log.debug("No publishable outbox events found");
            return 0;
        }

        int sent = 0;
        log.info("Outbox publisher batch started: size={}", events.size());
        for (OutboxEventEntity event : events) {
            try {
                publish(event);
                if (transactionService.markSent(event.getId())) {
                    sentCounter.increment();
                    sent++;
                    log.info("Outbox event sent: eventId={}, eventType={}", event.getId(), event.getEventType());
                } else {
                    log.warn("Outbox event success ignored after lease loss: eventId={}, eventType={}",
                            event.getId(), event.getEventType());
                }
            } catch (Exception exception) {
                markFailed(event, exception);
            }
        }
        log.info("Outbox publisher batch finished: locked={}, sent={}", events.size(), sent);
        return sent;
    }

    private void publish(OutboxEventEntity event) throws Exception {
        String topic = topicRouter.topicFor(event.getEventType());
        String payload = event.getPayload().toString();

        log.info(
                "Publishing outbox event: eventId={}, eventType={}, topic={}, key={}, retryCount={}",
                event.getId(),
                event.getEventType(),
                topic,
                event.getEventKey(),
                event.getRetryCount()
        );

        kafkaOperations.send(topic, event.getEventKey(), payload)
                .get(properties.sendTimeoutMs(), TimeUnit.MILLISECONDS);
    }

    private void markFailed(OutboxEventEntity event, Exception exception) {
        String lastError = shortError(exception);
        boolean markedFailed = transactionService.markFailed(
                event.getId(),
                lastError,
                Duration.ofMillis(properties.publishDelayMs())
        );
        if (!markedFailed) {
            log.warn("Outbox event failure ignored after lease loss: eventId={}, eventType={}, error={}",
                    event.getId(), event.getEventType(), lastError);
            return;
        }

        failedCounter.increment();
        retryCounter.increment();
        log.warn(
                "Outbox event publish failed: eventId={}, eventType={}, retryCount={}, error={}",
                event.getId(),
                event.getEventType(),
                event.getRetryCount() + 1,
                lastError
        );
    }

    private void recoverStaleProcessing() {
        int recovered = transactionService.recoverStaleProcessing(
                Duration.ofMillis(properties.processingTimeoutMs())
        );
        if (recovered == 0) {
            return;
        }
        processingRecoveredCounter.increment(recovered);
        log.warn("Recovered stale outbox processing events: count={}", recovered);
    }

    private String shortError(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }
        return message.length() <= MAX_ERROR_LENGTH ? message : message.substring(0, MAX_ERROR_LENGTH);
    }
}
