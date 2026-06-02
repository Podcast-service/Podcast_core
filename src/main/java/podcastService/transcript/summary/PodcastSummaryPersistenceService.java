package podcastService.transcript.summary;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import podcastService.podcast.entity.PodcastEntity;
import podcastService.podcast.repository.PodcastRepository;
import podcastService.transcript.dto.PodcastSummaryResponse;
import podcastService.transcript.entity.PodcastSummaryEntity;
import podcastService.transcript.entity.PodcastSummaryId;
import podcastService.transcript.repository.PodcastSummaryRepository;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PodcastSummaryPersistenceService {

    private final PodcastRepository podcastRepository;
    private final PodcastSummaryRepository podcastSummaryRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PodcastSummaryResponse saveFinalSummary(
            UUID podcastId,
            String language,
            String content,
            boolean force
    ) {
        String normalizedContent = normalizeSummary(content);

        if (!force) {
            PodcastSummaryEntity existing = podcastSummaryRepository
                    .findByIdPodcastIdAndIdLanguage(podcastId, language)
                    .orElse(null);
            if (existing != null) {
                log.info("Podcast summary appeared during generation, podcastId={}, language={}",
                        podcastId, language);
                return toResponse(existing);
            }
        }

        PodcastSummaryEntity summary = podcastSummaryRepository
                .findByIdPodcastIdAndIdLanguage(podcastId, language)
                .orElseGet(() -> {
                    PodcastSummaryEntity created = new PodcastSummaryEntity();
                    created.setId(new PodcastSummaryId(podcastId, language));
                    PodcastEntity podcastReference = podcastRepository.getReferenceById(podcastId);
                    created.setPodcast(podcastReference);
                    return created;
                });

        summary.setContent(normalizedContent);
        summary.setGeneratedAt(OffsetDateTime.now());

        try {
            PodcastSummaryEntity saved = podcastSummaryRepository.saveAndFlush(summary);
            log.info("Podcast summary saved, podcastId={}, language={}, force={}, summaryLength={}",
                    podcastId, language, force, normalizedContent.length());
            return toResponse(saved);
        } catch (DataIntegrityViolationException exception) {
            PodcastSummaryEntity existing = podcastSummaryRepository
                    .findByIdPodcastIdAndIdLanguage(podcastId, language)
                    .orElseThrow(() -> exception);
            log.info("Podcast summary save conflict resolved by loading existing summary, podcastId={}, language={}",
                    podcastId, language);
            return toResponse(existing);
        }
    }

    private String normalizeSummary(String content) {
        if (content == null || content.isBlank()) {
            throw new OpenRouterClientException("OpenRouter returned blank summary");
        }
        return content.trim();
    }

    private PodcastSummaryResponse toResponse(PodcastSummaryEntity summary) {
        return new PodcastSummaryResponse(
                summary.getId().getPodcastId(),
                summary.getId().getLanguage(),
                summary.getContent(),
                summary.getGeneratedAt()
        );
    }
}
