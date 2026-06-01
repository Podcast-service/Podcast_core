package podcastService.infrastructure.outbox.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import podcastService.infrastructure.outbox.entity.OutboxEventEntity;

import java.util.List;
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
}
