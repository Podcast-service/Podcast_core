package podcastService.transcript.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import podcastService.transcript.entity.PodcastSummaryEntity;
import podcastService.transcript.entity.PodcastSummaryId;

import java.util.Optional;
import java.util.UUID;

public interface PodcastSummaryRepository extends JpaRepository<PodcastSummaryEntity, PodcastSummaryId> {

    Optional<PodcastSummaryEntity> findByIdPodcastIdAndIdLanguage(UUID podcastId, String language);

    boolean existsByIdPodcastId(UUID podcastId);
}
