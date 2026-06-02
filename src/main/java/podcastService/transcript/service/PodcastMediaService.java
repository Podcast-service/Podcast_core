package podcastService.transcript.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import podcastService.common.exception.NotFoundException;
import podcastService.podcast.entity.PodcastEntity;
import podcastService.podcast.entity.Status;
import podcastService.podcast.repository.PodcastRepository;
import podcastService.transcript.dto.PodcastSummaryResponse;
import podcastService.transcript.dto.PodcastTranscriptResponse;
import podcastService.transcript.entity.PodcastSummaryEntity;
import podcastService.transcript.entity.PodcastTranscriptEntity;
import podcastService.transcript.repository.PodcastSummaryRepository;
import podcastService.transcript.repository.PodcastTranscriptRepository;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PodcastMediaService {

    private static final String DEFAULT_LANGUAGE = "RU";

    private final PodcastRepository podcastRepository;
    private final PodcastTranscriptRepository podcastTranscriptRepository;
    private final PodcastSummaryRepository podcastSummaryRepository;
    private final PodcastTranscriptContentResolver podcastTranscriptContentResolver;

    @Transactional(readOnly = true)
    public PodcastTranscriptResponse getTranscript(UUID podcastId) {
        requirePublishedPodcast(podcastId);

        PodcastTranscriptEntity transcript = podcastTranscriptRepository
                .findByIdPodcastIdAndIdLanguage(podcastId, DEFAULT_LANGUAGE)
                .orElseThrow(() -> new NotFoundException("Podcast transcript not found"));

        log.debug("Podcast transcript loaded: podcastId={}, language={}", podcastId, transcript.getId().getLanguage());
        return new PodcastTranscriptResponse(
                transcript.getId().getPodcastId(),
                transcript.getId().getLanguage(),
                podcastTranscriptContentResolver.resolve(transcript.getContent()),
                transcript.getGeneratedAt()
        );
    }

    @Transactional(readOnly = true)
    public PodcastSummaryResponse getSummary(UUID podcastId) {
        requirePublishedPodcast(podcastId);

        PodcastSummaryEntity summary = podcastSummaryRepository
                .findByIdPodcastIdAndIdLanguage(podcastId, DEFAULT_LANGUAGE)
                .orElseThrow(() -> new NotFoundException("Podcast summary not found"));

        log.debug("Podcast summary loaded: podcastId={}, language={}", podcastId, summary.getId().getLanguage());
        return new PodcastSummaryResponse(
                summary.getId().getPodcastId(),
                summary.getId().getLanguage(),
                summary.getContent(),
                summary.getGeneratedAt()
        );
    }

    private void requirePublishedPodcast(UUID podcastId) {
        PodcastEntity podcast = podcastRepository.findById(podcastId)
                .orElseThrow(() -> new NotFoundException("Podcast not found"));

        if (podcast.getStatus() != Status.PUBLISHED) {
            throw new NotFoundException("Podcast not found");
        }
    }
}
