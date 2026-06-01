package podcastService.infrastructure.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import podcastService.infrastructure.outbox.entity.OutboxEventEntity;
import podcastService.infrastructure.outbox.repository.OutboxEventRepository;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxPublisherTransactionService {

    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public List<OutboxEventEntity> claimNextPublishBatch(int batchSize, int maxRetryAttempts) {
        List<OutboxEventEntity> events = outboxEventRepository.lockNextPublishBatch(batchSize, maxRetryAttempts);
        if (events.isEmpty()) {
            return List.of();
        }

        OffsetDateTime processingStartedAt = OffsetDateTime.now(ZoneOffset.UTC);
        events.forEach(event -> {
            event.setStatus(OutboxEventStatus.PROCESSING);
            event.setProcessingStartedAt(processingStartedAt);
        });
        return outboxEventRepository.saveAllAndFlush(events);
    }

    @Transactional
    public boolean markSent(UUID eventId) {
        return outboxEventRepository.markSent(eventId) == 1;
    }

    @Transactional
    public boolean markFailed(UUID eventId, String lastError, Duration retryBackoff) {
        OffsetDateTime availableAt = OffsetDateTime.now(ZoneOffset.UTC).plus(retryBackoff);
        return outboxEventRepository.markFailed(eventId, lastError, availableAt) == 1;
    }

    @Transactional
    public int recoverStaleProcessing(Duration processingTimeout) {
        OffsetDateTime staleBefore = OffsetDateTime.now(ZoneOffset.UTC).minus(processingTimeout);
        return outboxEventRepository.recoverStaleProcessing(staleBefore);
    }
}
