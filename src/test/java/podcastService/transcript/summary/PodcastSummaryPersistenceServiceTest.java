package podcastService.transcript.summary;

import org.junit.jupiter.api.Test;
import podcastService.podcast.entity.PodcastEntity;
import podcastService.podcast.repository.PodcastRepository;
import podcastService.transcript.dto.PodcastSummaryResponse;
import podcastService.transcript.entity.PodcastSummaryEntity;
import podcastService.transcript.entity.PodcastSummaryId;
import podcastService.transcript.repository.PodcastSummaryRepository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PodcastSummaryPersistenceServiceTest {

    private static final UUID PODCAST_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final String LANGUAGE = "RU";

    private final PodcastRepository podcastRepository = mock(PodcastRepository.class);
    private final PodcastSummaryRepository podcastSummaryRepository = mock(PodcastSummaryRepository.class);
    private final PodcastSummaryPersistenceService service = new PodcastSummaryPersistenceService(
            podcastRepository,
            podcastSummaryRepository
    );

    @Test
    void createsSummaryInPersistenceService() {
        PodcastEntity podcast = podcast();
        when(podcastSummaryRepository.findByIdPodcastIdAndIdLanguage(PODCAST_ID, LANGUAGE))
                .thenReturn(Optional.empty());
        when(podcastRepository.getReferenceById(PODCAST_ID)).thenReturn(podcast);
        when(podcastSummaryRepository.saveAndFlush(any(PodcastSummaryEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PodcastSummaryResponse response = service.saveFinalSummary(PODCAST_ID, LANGUAGE, " Generated summary ", false);

        assertThat(response.content()).isEqualTo("Generated summary");
        verify(podcastRepository).getReferenceById(PODCAST_ID);
        verify(podcastSummaryRepository).saveAndFlush(any(PodcastSummaryEntity.class));
    }

    @Test
    void forceFalseReturnsExistingSummaryWithoutRegenerationSave() {
        PodcastSummaryEntity existing = summary("Existing summary");
        when(podcastSummaryRepository.findByIdPodcastIdAndIdLanguage(PODCAST_ID, LANGUAGE))
                .thenReturn(Optional.of(existing));

        PodcastSummaryResponse response = service.saveFinalSummary(PODCAST_ID, LANGUAGE, "New summary", false);

        assertThat(response.content()).isEqualTo("Existing summary");
        verify(podcastSummaryRepository, never()).saveAndFlush(any());
        verify(podcastRepository, never()).getReferenceById(PODCAST_ID);
    }

    @Test
    void forceTrueUpdatesExistingSummary() {
        PodcastSummaryEntity existing = summary("Old summary");
        when(podcastSummaryRepository.findByIdPodcastIdAndIdLanguage(PODCAST_ID, LANGUAGE))
                .thenReturn(Optional.of(existing));
        when(podcastSummaryRepository.saveAndFlush(existing)).thenReturn(existing);

        PodcastSummaryResponse response = service.saveFinalSummary(PODCAST_ID, LANGUAGE, "New summary", true);

        assertThat(response.content()).isEqualTo("New summary");
        assertThat(existing.getContent()).isEqualTo("New summary");
        assertThat(existing.getGeneratedAt()).isNotNull();
        verify(podcastSummaryRepository).saveAndFlush(existing);
    }

    private PodcastEntity podcast() {
        PodcastEntity podcast = new PodcastEntity();
        podcast.setId(PODCAST_ID);
        return podcast;
    }

    private PodcastSummaryEntity summary(String content) {
        PodcastSummaryEntity summary = new PodcastSummaryEntity();
        summary.setId(new PodcastSummaryId(PODCAST_ID, LANGUAGE));
        summary.setPodcast(podcast());
        summary.setContent(content);
        summary.setGeneratedAt(OffsetDateTime.parse("2026-06-01T00:00:00Z"));
        return summary;
    }
}
