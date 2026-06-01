package podcastService.infrastructure.outbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import podcastService.infrastructure.outbox.entity.OutboxEventEntity;
import podcastService.infrastructure.outbox.repository.OutboxEventRepository;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxPublisherTransactionServiceTest {

    private static final UUID EVENT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");

    private OutboxEventRepository repository;
    private OutboxPublisherTransactionService service;

    @BeforeEach
    void setUp() {
        repository = mock(OutboxEventRepository.class);
        service = new OutboxPublisherTransactionService(repository);
    }

    @Test
    void claimMovesLockedEventsToProcessingBeforeReturning() {
        OutboxEventEntity event = new OutboxEventEntity();
        event.setId(EVENT_ID);
        when(repository.lockNextPublishBatch(100, 10)).thenReturn(List.of(event));
        when(repository.saveAllAndFlush(List.of(event))).thenReturn(List.of(event));

        List<OutboxEventEntity> claimed = service.claimNextPublishBatch(100, 10);

        assertThat(claimed).containsExactly(event);
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PROCESSING);
        assertThat(event.getProcessingStartedAt()).isNotNull();
    }

    @Test
    void markFailedDelegatesRetryIncrementAndBackoffToRepositoryUpdate() {
        when(repository.markFailed(eq(EVENT_ID), eq("broker down"), any(OffsetDateTime.class))).thenReturn(1);
        OffsetDateTime before = OffsetDateTime.now().plusSeconds(2);

        boolean marked = service.markFailed(EVENT_ID, "broker down", Duration.ofSeconds(3));

        assertThat(marked).isTrue();
        ArgumentCaptor<OffsetDateTime> availableAt = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(repository).markFailed(eq(EVENT_ID), eq("broker down"), availableAt.capture());
        assertThat(availableAt.getValue()).isAfter(before);
    }

    @Test
    void staleProcessingRecoveryUsesConfiguredTimeout() {
        when(repository.recoverStaleProcessing(any(OffsetDateTime.class))).thenReturn(3);

        int recovered = service.recoverStaleProcessing(Duration.ofMinutes(10));

        assertThat(recovered).isEqualTo(3);
        verify(repository).recoverStaleProcessing(any(OffsetDateTime.class));
    }
}
