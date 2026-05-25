package podcastService.transcript.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import podcastService.transcript.entity.PodcastTranscriptEntity;
import podcastService.transcript.entity.PodcastTranscriptId;

import java.util.Optional;
import java.util.UUID;

public interface PodcastTranscriptRepository extends JpaRepository<PodcastTranscriptEntity, PodcastTranscriptId> {

    Optional<PodcastTranscriptEntity> findByIdPodcastIdAndIdLanguage(UUID podcastId, String language);

    boolean existsByIdPodcastId(UUID podcastId);

    boolean existsByIdPodcastIdAndContentIsNotNull(UUID podcastId);
}
