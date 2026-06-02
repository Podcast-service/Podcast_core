package podcastService.infrastructure.outbox.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import podcastService.infrastructure.outbox.entity.OutboxEventEntity;

import java.util.List;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {

    @Query(
            value = """
                    select *
                    from outbox_events
                    where status in ('NEW', 'FAILED')
                      and available_at <= now()
                      and retry_count < :maxRetryAttempts
                    order by available_at, created_at
                    limit :batchSize
                    for update skip locked
                    """,
            nativeQuery = true
    )
    List<OutboxEventEntity> lockNextPublishBatch(
            @Param("batchSize") int batchSize,
            @Param("maxRetryAttempts") int maxRetryAttempts
    );

    @Query(
            value = """
                    select count(*)
                    from outbox_events
                    where status in ('NEW', 'FAILED')
                      and available_at <= now()
                      and retry_count < :maxRetryAttempts
                    """,
            nativeQuery = true
    )
    long countPublishable(@Param("maxRetryAttempts") int maxRetryAttempts);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            value = """
                    update outbox_events
                       set status = 'SENT',
                           sent_at = now(),
                           last_error = null,
                           processing_started_at = null
                     where id = :eventId
                       and status = 'PROCESSING'
                    """,
            nativeQuery = true
    )
    int markSent(@Param("eventId") UUID eventId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            value = """
                    update outbox_events
                       set status = 'FAILED',
                           retry_count = retry_count + 1,
                           last_error = :lastError,
                           available_at = :availableAt,
                           processing_started_at = null
                     where id = :eventId
                       and status = 'PROCESSING'
                    """,
            nativeQuery = true
    )
    int markFailed(
            @Param("eventId") UUID eventId,
            @Param("lastError") String lastError,
            @Param("availableAt") OffsetDateTime availableAt
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            value = """
                    update outbox_events
                       set status = 'FAILED',
                           retry_count = retry_count + 1,
                           last_error = 'stale processing recovered',
                           available_at = now(),
                           processing_started_at = null
                     where status = 'PROCESSING'
                       and coalesce(processing_started_at, created_at) <= :staleBefore
                    """,
            nativeQuery = true
    )
    int recoverStaleProcessing(@Param("staleBefore") OffsetDateTime staleBefore);
}
