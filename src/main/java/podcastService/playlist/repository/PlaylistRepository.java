package podcastService.playlist.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import podcastService.playlist.entity.PlaylistEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlaylistRepository extends
        JpaRepository<PlaylistEntity, UUID>,
        JpaSpecificationExecutor<PlaylistEntity> {

    @EntityGraph(attributePaths = "owner")
    @Query("select p from PlaylistEntity p where p.id = :id")
    Optional<PlaylistEntity> findWithOwnerById(UUID id);

    @EntityGraph(attributePaths = "owner")
    @Query("select p from PlaylistEntity p where p.id in :ids")
    List<PlaylistEntity> findWithOwnerByIdIn(@Param("ids") Collection<UUID> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "owner")
    @Query("select p from PlaylistEntity p where p.id = :id")
    Optional<PlaylistEntity> findWithOwnerByIdForUpdate(UUID id);
}
