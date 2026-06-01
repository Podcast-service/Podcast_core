package podcastService.infrastructure.outbox.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import podcastService.infrastructure.outbox.entity.OutboxEventEntity;

import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {
}
