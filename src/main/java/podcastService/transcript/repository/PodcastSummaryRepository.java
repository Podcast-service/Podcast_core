package podcastService.transcript.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import podcastService.transcript.entity.PodcastSummaryEntity;
import podcastService.transcript.entity.PodcastSummaryId;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface PodcastSummaryRepository extends JpaRepository<PodcastSummaryEntity, PodcastSummaryId> {

    Optional<PodcastSummaryEntity> findByIdPodcastIdAndIdLanguage(UUID podcastId, String language);

    boolean existsByIdPodcastId(UUID podcastId);

    @Query("""
            select distinct s.id.podcastId
            from PodcastSummaryEntity s
            where s.id.podcastId in :podcastIds
            """)
    Set<UUID> findPodcastIdsWithSummary(@Param("podcastIds") Collection<UUID> podcastIds);
}
