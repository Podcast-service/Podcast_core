package podcastService.subscription.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import podcastService.subscription.entity.SubscriptionEntity;
import podcastService.subscription.entity.SubscriptionId;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, SubscriptionId> {

    boolean existsByIdSubscriberProfileIdAndIdAuthorId(UUID subscriberProfileId, UUID authorId);

    @EntityGraph(attributePaths = {
            "author",
            "author.userProfile"
    })
    Page<SubscriptionEntity> findByIdSubscriberProfileIdOrderBySubscribedAtDesc(
            UUID subscriberProfileId,
            Pageable pageable
    );

    @Query("""
            select s.id.authorId
            from SubscriptionEntity s
            where s.id.subscriberProfileId = :subscriberProfileId
              and s.id.authorId in :authorIds
            """)
    Set<UUID> findSubscribedAuthorIds(
            @Param("subscriberProfileId") UUID subscriberProfileId,
            @Param("authorIds") Collection<UUID> authorIds
    );
}
