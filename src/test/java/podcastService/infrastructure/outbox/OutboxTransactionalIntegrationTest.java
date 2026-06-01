package podcastService.infrastructure.outbox;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import podcastService.infrastructure.outbox.entity.OutboxEventEntity;
import podcastService.infrastructure.outbox.recommendation.RecommendationEventTypes;
import podcastService.infrastructure.outbox.recommendation.RecommendationOutboxEventService;
import podcastService.infrastructure.outbox.repository.OutboxEventRepository;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=false",
        "app.outbox.publisher.enabled=false",
        "app.kafka.producer.enabled=false",
        "app.recommendation.events.enabled=false"
})
@Testcontainers(disabledWithoutDocker = true)
class OutboxTransactionalIntegrationTest {

    private static final UUID PODCAST_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("podcast_db")
            .withUsername("podcast_user")
            .withPassword("podcast_pass");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private OutboxEventService outboxEventService;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxPublisherTransactionService transactionService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void cleanOutbox() {
        outboxEventRepository.deleteAll();
        outboxEventRepository.flush();
    }

    @Test
    void recommendationFlagOffDoesNotCreateOutboxEvent() {
        recommendationOutboxService(false).saveUserActivityEvent(USER_ID, envelope());

        assertThat(outboxEventRepository.count()).isZero();
    }

    @Test
    void recommendationFlagOnCreatesOutboxEvent() {
        recommendationOutboxService(true).saveUserActivityEvent(USER_ID, envelope());

        assertThat(outboxEventRepository.count()).isEqualTo(1);
    }

    @Test
    void businessTransactionRollbackAlsoRollsBackOutboxEvent() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> transaction.executeWithoutResult(ignored -> {
            recommendationOutboxService(true).saveUserActivityEvent(USER_ID, envelope());
            throw new IllegalStateException("rollback business action");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(outboxEventRepository.count()).isZero();
    }

    @Test
    void claimAndSuccessFinalizeUseSeparateCommittedTransitions() {
        OutboxEventEntity event = saveNewEvent();

        List<OutboxEventEntity> claimed = transactionService.claimNextPublishBatch(10, 10);

        assertThat(claimed).extracting(OutboxEventEntity::getId).containsExactly(event.getId());
        OutboxEventEntity processing = reload(event.getId());
        assertThat(processing.getStatus()).isEqualTo(OutboxEventStatus.PROCESSING);
        assertThat(processing.getProcessingStartedAt()).isNotNull();

        assertThat(transactionService.markSent(event.getId())).isTrue();

        OutboxEventEntity sent = reload(event.getId());
        assertThat(sent.getStatus()).isEqualTo(OutboxEventStatus.SENT);
        assertThat(sent.getSentAt()).isNotNull();
        assertThat(sent.getProcessingStartedAt()).isNull();
    }

    @Test
    void failedFinalizeIncrementsRetryAndSchedulesBackoff() {
        OutboxEventEntity event = saveNewEvent();
        transactionService.claimNextPublishBatch(10, 10);
        OffsetDateTime before = OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(20);

        assertThat(transactionService.markFailed(event.getId(), "broker down", Duration.ofSeconds(30))).isTrue();

        OutboxEventEntity failed = reload(event.getId());
        assertThat(failed.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
        assertThat(failed.getRetryCount()).isEqualTo(1);
        assertThat(failed.getLastError()).isEqualTo("broker down");
        assertThat(failed.getAvailableAt()).isAfter(before);
        assertThat(failed.getProcessingStartedAt()).isNull();
    }

    @Test
    void maxRetryAttemptEventsAreNotClaimed() {
        OutboxEventEntity event = reload(saveNewEvent().getId());
        event.setRetryCount(10);
        outboxEventRepository.saveAndFlush(event);

        assertThat(transactionService.claimNextPublishBatch(10, 10)).isEmpty();
    }

    @Test
    void staleProcessingRecoveryMovesEventBackToFailed() {
        OutboxEventEntity event = reload(saveNewEvent().getId());
        event.setStatus(OutboxEventStatus.PROCESSING);
        event.setProcessingStartedAt(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(11));
        outboxEventRepository.saveAndFlush(event);

        assertThat(transactionService.recoverStaleProcessing(Duration.ofMinutes(10))).isEqualTo(1);

        OutboxEventEntity recovered = reload(event.getId());
        assertThat(recovered.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
        assertThat(recovered.getRetryCount()).isEqualTo(1);
        assertThat(recovered.getLastError()).isEqualTo("stale processing recovered");
        assertThat(recovered.getProcessingStartedAt()).isNull();
    }

    private RecommendationOutboxEventService recommendationOutboxService(boolean enabled) {
        return new RecommendationOutboxEventService(
                outboxEventService,
                new RecommendationEventsProperties(enabled)
        );
    }

    private OutboxEventEntity saveNewEvent() {
        return outboxEventService.saveEvent("USER_ACTIVITY", USER_ID, USER_ID.toString(), envelope());
    }

    private DomainEventEnvelope envelope() {
        return new DomainEventEnvelope(
                UUID.randomUUID(),
                RecommendationEventTypes.PODCAST_LIKED,
                1,
                "podcast-core",
                Instant.now(),
                null,
                null,
                USER_ID,
                Map.of("podcastId", PODCAST_ID, "userId", USER_ID)
        );
    }

    private OutboxEventEntity reload(UUID eventId) {
        entityManager.clear();
        return outboxEventRepository.findById(eventId).orElseThrow();
    }
}
