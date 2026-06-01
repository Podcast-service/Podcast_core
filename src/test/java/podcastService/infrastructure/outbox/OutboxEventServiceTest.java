package podcastService.infrastructure.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import podcastService.infrastructure.config.JacksonConfig;
import podcastService.infrastructure.outbox.entity.OutboxEventEntity;
import podcastService.infrastructure.outbox.repository.OutboxEventRepository;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxEventServiceTest {

    private static final UUID EVENT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID PODCAST_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final OffsetDateTime OCCURRED_AT = OffsetDateTime.parse("2026-06-01T10:15:30Z");

    private OutboxEventRepository repository;
    private OutboxEventService service;

    @BeforeEach
    void setUp() {
        repository = mock(OutboxEventRepository.class);
        service = new OutboxEventService(repository, new JacksonConfig().objectMapper());
    }

    @Test
    void outboxEventEntityHasSafeCreationDefaults() {
        OutboxEventEntity entity = new OutboxEventEntity();

        assertThat(entity.getId()).isNotNull();
        assertThat(entity.getEventVersion()).isEqualTo(1);
        assertThat(entity.getStatus()).isEqualTo(OutboxEventStatus.NEW);
        assertThat(entity.getRetryCount()).isZero();
    }

    @Test
    void saveEventSerializesDomainEventEnvelopePayload() {
        when(repository.saveAndFlush(any(OutboxEventEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OutboxEventEntity saved = service.saveEvent(
                " podcast ",
                PODCAST_ID,
                " podcast-created ",
                envelope()
        );

        JsonNode payload = saved.getPayload();
        assertThat(payload.get("eventId").asText()).isEqualTo(EVENT_ID.toString());
        assertThat(payload.get("eventType").asText()).isEqualTo("podcast.created");
        assertThat(payload.get("eventVersion").asInt()).isEqualTo(1);
        assertThat(payload.get("producer").asText()).isEqualTo("podcast-core");
        assertThat(payload.get("userId").asText()).isEqualTo(USER_ID.toString());
        assertThat(payload.get("payload").get("podcastId").asText()).isEqualTo(PODCAST_ID.toString());
        assertThat(payload.get("payload").get("title").asText()).isEqualTo("Outbox intro");
    }

    @Test
    void saveEventPersistsNewOutboxEventThroughRepository() {
        when(repository.saveAndFlush(any(OutboxEventEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OutboxEventEntity saved = service.saveEvent(
                "podcast",
                PODCAST_ID,
                "podcast-created",
                envelope()
        );

        ArgumentCaptor<OutboxEventEntity> captor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(repository).saveAndFlush(captor.capture());

        OutboxEventEntity persisted = captor.getValue();
        assertThat(saved).isSameAs(persisted);
        assertThat(persisted.getId()).isEqualTo(EVENT_ID);
        assertThat(persisted.getAggregateType()).isEqualTo("podcast");
        assertThat(persisted.getAggregateId()).isEqualTo(PODCAST_ID);
        assertThat(persisted.getEventType()).isEqualTo("podcast.created");
        assertThat(persisted.getEventVersion()).isEqualTo(1);
        assertThat(persisted.getEventKey()).isEqualTo("podcast-created");
        assertThat(persisted.getStatus()).isEqualTo(OutboxEventStatus.NEW);
        assertThat(persisted.getRetryCount()).isZero();
        assertThat(persisted.getHeaders()).isNull();
    }

    private DomainEventEnvelope envelope() {
        return new DomainEventEnvelope(
                EVENT_ID,
                "podcast.created",
                1,
                "podcast-core",
                OCCURRED_AT,
                "correlation-1",
                "causation-1",
                USER_ID,
                Map.of(
                        "podcastId", PODCAST_ID,
                        "title", "Outbox intro"
                )
        );
    }
}
