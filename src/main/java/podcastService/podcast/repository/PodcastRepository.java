package podcastService.podcast.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import podcastService.podcast.entity.PodcastEntity;

import java.util.Optional;
import java.util.UUID;

public interface PodcastRepository extends
        JpaRepository<PodcastEntity, UUID>,
        JpaSpecificationExecutor<PodcastEntity> {

    @EntityGraph(attributePaths = {
            "author",
            "author.userProfile",
            "category"
    })
    @Query("select p from PodcastEntity p where p.id = :id")
    Optional<PodcastEntity> findDetailedById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
            "author",
            "author.userProfile",
            "category"
    })
    @Query("select p from PodcastEntity p where p.id = :id")
    Optional<PodcastEntity> findDetailedByIdForUpdate(UUID id);
}
