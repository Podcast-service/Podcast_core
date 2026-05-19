package podcastService.transcript.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import podcastService.common.exception.NotFoundException;
import podcastService.podcast.entity.PodcastEntity;
import podcastService.podcast.entity.Status;
import podcastService.podcast.repository.PodcastRepository;
import podcastService.transcript.dto.PodcastSummaryResponse;
import podcastService.transcript.dto.PodcastTranscriptResponse;
import podcastService.transcript.entity.PodcastSummaryEntity;
import podcastService.transcript.entity.PodcastSummaryId;
import podcastService.transcript.entity.PodcastTranscriptEntity;
import podcastService.transcript.entity.PodcastTranscriptId;
import podcastService.transcript.repository.PodcastSummaryRepository;
import podcastService.transcript.repository.PodcastTranscriptRepository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PodcastMediaServiceTest {

    private static final UUID PODCAST_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");

    @Mock
    private PodcastRepository podcastRepository;

    @Mock
    private PodcastTranscriptRepository podcastTranscriptRepository;

    @Mock
    private PodcastSummaryRepository podcastSummaryRepository;

    private PodcastMediaService service;

    @BeforeEach
    void setUp() {
        service = new PodcastMediaService(podcastRepository, podcastTranscriptRepository, podcastSummaryRepository);
    }

    @Test
    void getTranscriptReturnsPublishedPodcastTranscript() {
        PodcastTranscriptEntity transcript = transcript("Расшифровка выпуска");
        when(podcastRepository.findById(PODCAST_ID)).thenReturn(Optional.of(podcast(Status.PUBLISHED)));
        when(podcastTranscriptRepository.findByIdPodcastIdAndIdLanguage(PODCAST_ID, "RU"))
                .thenReturn(Optional.of(transcript));

        PodcastTranscriptResponse response = service.getTranscript(PODCAST_ID);

        assertThat(response.podcastId()).isEqualTo(PODCAST_ID);
        assertThat(response.language()).isEqualTo("RU");
        assertThat(response.content()).isEqualTo("Расшифровка выпуска");
    }

    @Test
    void getSummaryReturnsPublishedPodcastSummary() {
        PodcastSummaryEntity summary = summary("Краткое содержание");
        when(podcastRepository.findById(PODCAST_ID)).thenReturn(Optional.of(podcast(Status.PUBLISHED)));
        when(podcastSummaryRepository.findByIdPodcastIdAndIdLanguage(PODCAST_ID, "RU"))
                .thenReturn(Optional.of(summary));

        PodcastSummaryResponse response = service.getSummary(PODCAST_ID);

        assertThat(response.podcastId()).isEqualTo(PODCAST_ID);
        assertThat(response.language()).isEqualTo("RU");
        assertThat(response.content()).isEqualTo("Краткое содержание");
    }

    @Test
    void getTranscriptDoesNotExposeUnpublishedPodcast() {
        when(podcastRepository.findById(PODCAST_ID)).thenReturn(Optional.of(podcast(Status.PROCESSING)));

        assertThatThrownBy(() -> service.getTranscript(PODCAST_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Podcast not found");

        verify(podcastTranscriptRepository, never()).findByIdPodcastIdAndIdLanguage(PODCAST_ID, "RU");
    }

    @Test
    void getSummaryReturnsNotFoundWhenSummaryIsNotReady() {
        when(podcastRepository.findById(PODCAST_ID)).thenReturn(Optional.of(podcast(Status.PUBLISHED)));
        when(podcastSummaryRepository.findByIdPodcastIdAndIdLanguage(PODCAST_ID, "RU"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSummary(PODCAST_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Podcast summary not found");
    }

    private PodcastEntity podcast(Status status) {
        PodcastEntity podcast = new PodcastEntity();
        podcast.setId(PODCAST_ID);
        podcast.setStatus(status);
        return podcast;
    }

    private PodcastTranscriptEntity transcript(String content) {
        PodcastTranscriptEntity transcript = new PodcastTranscriptEntity();
        transcript.setId(new PodcastTranscriptId(PODCAST_ID, "RU"));
        transcript.setContent(content);
        transcript.setGeneratedAt(OffsetDateTime.parse("2026-05-13T10:15:30Z"));
        return transcript;
    }

    private PodcastSummaryEntity summary(String content) {
        PodcastSummaryEntity summary = new PodcastSummaryEntity();
        summary.setId(new PodcastSummaryId(PODCAST_ID, "RU"));
        summary.setContent(content);
        summary.setGeneratedAt(OffsetDateTime.parse("2026-05-13T10:15:30Z"));
        return summary;
    }
}
