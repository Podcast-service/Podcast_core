package podcastService.infrastructure.outbox;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import podcastService.infrastructure.messaging.config.KafkaMessagingProperties;
import podcastService.infrastructure.outbox.entity.OutboxEventEntity;
import podcastService.infrastructure.outbox.repository.OutboxEventRepository;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "app.outbox.publisher", name = "enabled", havingValue = "true")
public class OutboxPublisher {

    private static final int MAX_ERROR_LENGTH = 1_000;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaOperations<Object, Object> kafkaOperations;
    private final KafkaMessagingProperties kafkaMessagingProperties;
    private final OutboxPublisherProperties properties;
    private final OutboxEventTopicRouter topicRouter;
    private final Counter sentCounter;
    private final Counter failedCounter;
    private final Counter retryCounter;

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            KafkaOperations<Object, Object> kafkaOperations,
            KafkaMessagingProperties kafkaMessagingProperties,
            OutboxPublisherProperties properties,
            OutboxEventTopicRouter topicRouter,
            MeterRegistry meterRegistry
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaOperations = kafkaOperations;
        this.kafkaMessagingProperties = kafkaMessagingProperties;
        this.properties = properties;
        this.topicRouter = topicRouter;
        this.sentCounter = Counter.builder("outbox.events.sent").register(meterRegistry);
        this.failedCounter = Counter.builder("outbox.events.failed").register(meterRegistry);
        this.retryCounter = Counter.builder("outbox.events.retry").register(meterRegistry);
        Gauge.builder("outbox.events.pending", outboxEventRepository,
                        repository -> repository.countPublishable(properties.maxRetryAttempts()))
                .register(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${app.outbox.publisher.publish-delay-ms:3000}")
    public void publishScheduledBatch() {
        publishBatch();
    }

    @Transactional
    public int publishBatch() {
        if (!properties.enabled()) {
            log.debug("Outbox publisher disabled; skipping batch");
            return 0;
        }

        if (!kafkaMessagingProperties.getProducer().isEnabled()) {
            log.debug("Kafka producer disabled; skipping outbox batch");
            return 0;
        }

        List<OutboxEventEntity> events = outboxEventRepository.lockNextPublishBatch(
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
            event.setStatus(OutboxEventStatus.PROCESSING);
            outboxEventRepository.saveAndFlush(event);
            try {
                publish(event);
                markSent(event);
                sentCounter.increment();
                sent++;
            } catch (Exception exception) {
                markFailed(event, exception);
                failedCounter.increment();
                retryCounter.increment();
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

        kafkaOperations.send(topic, event.getEventKey(), payload).get();
    }

    private void markSent(OutboxEventEntity event) {
        event.setStatus(OutboxEventStatus.SENT);
        event.setSentAt(OffsetDateTime.now());
        event.setLastError(null);
        outboxEventRepository.saveAndFlush(event);
        log.info("Outbox event sent: eventId={}, eventType={}", event.getId(), event.getEventType());
    }

    private void markFailed(OutboxEventEntity event, Exception exception) {
        event.setStatus(OutboxEventStatus.FAILED);
        event.setRetryCount(event.getRetryCount() + 1);
        event.setLastError(shortError(exception));
        event.setAvailableAt(OffsetDateTime.now().plus(Duration.ofMillis(properties.publishDelayMs())));
        outboxEventRepository.saveAndFlush(event);
        log.warn(
                "Outbox event publish failed: eventId={}, eventType={}, retryCount={}, availableAt={}, error={}",
                event.getId(),
                event.getEventType(),
                event.getRetryCount(),
                event.getAvailableAt(),
                event.getLastError()
        );
    }

    private String shortError(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }
        return message.length() <= MAX_ERROR_LENGTH ? message : message.substring(0, MAX_ERROR_LENGTH);
    }
}
