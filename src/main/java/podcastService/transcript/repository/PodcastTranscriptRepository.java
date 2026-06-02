package podcastService.transcript.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import podcastService.podcast.entity.Status;
import podcastService.transcript.entity.PodcastTranscriptEntity;
import podcastService.transcript.entity.PodcastTranscriptId;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface PodcastTranscriptRepository extends JpaRepository<PodcastTranscriptEntity, PodcastTranscriptId> {

    Optional<PodcastTranscriptEntity> findByIdPodcastIdAndIdLanguage(UUID podcastId, String language);

    Optional<PodcastTranscriptEntity> findByIdPodcastIdAndIdLanguageAndPodcastStatus(
            UUID podcastId,
            String language,
            Status status
    );

    boolean existsByIdPodcastId(UUID podcastId);

    boolean existsByIdPodcastIdAndContentIsNotNull(UUID podcastId);

    @Query("""
            select distinct t.id.podcastId
            from PodcastTranscriptEntity t
            where t.id.podcastId in :podcastIds
              and t.content is not null
            """)
    Set<UUID> findPodcastIdsWithContent(@Param("podcastIds") Collection<UUID> podcastIds);
}
