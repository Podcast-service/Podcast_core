package podcastService.podcast.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import podcastService.podcast.entity.PodcastEntity;
import podcastService.podcast.entity.Status;

import java.util.Collection;
import java.util.List;
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

    @EntityGraph(attributePaths = {
            "author",
            "author.userProfile",
            "category"
    })
    @Query("select p from PodcastEntity p where p.id in :ids")
    List<PodcastEntity> findDetailedByIdIn(@Param("ids") Collection<UUID> ids);

    @EntityGraph(attributePaths = {
            "author",
            "author.userProfile",
            "category"
    })
    @Query("""
            select p
            from PodcastEntity p
            where p.status = :status
              and p.author.id in (
                    select s.id.authorId
                    from SubscriptionEntity s
                    where s.id.subscriberProfileId = :subscriberProfileId
              )
            """)
    Page<PodcastEntity> findSubscriptionFeed(
            @Param("subscriberProfileId") UUID subscriberProfileId,
            @Param("status") Status status,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
            "author",
            "author.userProfile",
            "category"
    })
    @Query("select p from PodcastEntity p where p.id = :id")
    Optional<PodcastEntity> findDetailedByIdForUpdate(UUID id);
}
