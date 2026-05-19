package podcastService.podcast.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import podcastService.podcast.entity.PodcastVoteEntity;
import podcastService.podcast.entity.PodcastVoteId;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PodcastVoteRepository extends JpaRepository<PodcastVoteEntity, PodcastVoteId> {

    Optional<PodcastVoteEntity> findByIdUserProfileIdAndIdPodcastId(UUID userProfileId, UUID podcastId);

    @Query("""
            select v
            from PodcastVoteEntity v
            where v.id.userProfileId = :userProfileId
              and v.id.podcastId in :podcastIds
            """)
    List<PodcastVoteEntity> findByUserProfileIdAndPodcastIds(
            @Param("userProfileId") UUID userProfileId,
            @Param("podcastIds") Collection<UUID> podcastIds
    );
}
