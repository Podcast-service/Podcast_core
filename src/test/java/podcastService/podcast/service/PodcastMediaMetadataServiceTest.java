package podcastService.podcast.service;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import podcastService.podcast.entity.PodcastEntity;
import podcastService.podcast.entity.Status;
import podcastService.podcast.repository.PodcastRepository;
import podcastService.infrastructure.messaging.error.InvalidKafkaMessageException;
import podcastService.transcript.entity.PodcastTranscriptEntity;
import podcastService.transcript.entity.PodcastTranscriptId;
import podcastService.transcript.repository.PodcastTranscriptRepository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PodcastMediaMetadataServiceTest {

    private static final UUID PODCAST_ID = UUID.fromString("00000000-0000-0000-0000-000000000301");

    @Mock
    private PodcastRepository podcastRepository;

    @Mock
    private PodcastTranscriptRepository podcastTranscriptRepository;

    private PodcastMediaMetadataService service;

    @BeforeEach
    void setUp() {
        service = new PodcastMediaMetadataService(
                podcastRepository,
                podcastTranscriptRepository,
                JsonMapper.builder().addModule(new JavaTimeModule()).build(),
                new PodcastMediaStatusTransitionPolicy()
        );
    }

    @Test
    void startUploadMovesDraftToUploading() {
        PodcastEntity podcast = podcast(Status.DRAFT);
        when(podcastRepository.findDetailedByIdForUpdate(PODCAST_ID)).thenReturn(Optional.of(podcast));

        service.markFileUploadStarted(PODCAST_ID, null);

        assertThat(podcast.getStatus()).isEqualTo(Status.UPLOADING);
        verify(podcastRepository).saveAndFlush(podcast);
    }

    @Test
    void startUploadDoesNotDowngradeProcessed() {
        PodcastEntity podcast = podcast(Status.PROCESSED);
        when(podcastRepository.findDetailedByIdForUpdate(PODCAST_ID)).thenReturn(Optional.of(podcast));

        service.markFileUploadStarted(PODCAST_ID, null);

        assertThat(podcast.getStatus()).isEqualTo(Status.PROCESSED);
        verify(podcastRepository, never()).saveAndFlush(any(PodcastEntity.class));
    }

    @Test
    void uploadedDoesNotMutateMetadataWhenTransitionWouldDowngradeStatus() {
        PodcastEntity podcast = podcast(Status.PROCESSED);
        podcast.setAudioUrlFile("/media/original.mp3");
        podcast.setAudioSizeFile(100L);
        podcast.setDurationSeconds(1000L);
        when(podcastRepository.findDetailedByIdForUpdate(PODCAST_ID)).thenReturn(Optional.of(podcast));

        service.markFileUploaded(PODCAST_ID, "/media/stale.mp3", null);

        assertThat(podcast.getAudioUrlFile()).isEqualTo("/media/original.mp3");
        assertThat(podcast.getAudioSizeFile()).isEqualTo(100L);
        assertThat(podcast.getDurationSeconds()).isEqualTo(1000L);
        assertThat(podcast.getStatus()).isEqualTo(Status.PROCESSED);
        verify(podcastRepository, never()).saveAndFlush(any(PodcastEntity.class));
    }

    @Test
    void uploadedStoresSourceAudioMetadataAndMarksUploaded() {
        PodcastEntity podcast = podcast(Status.UPLOADING);
        when(podcastRepository.findDetailedByIdForUpdate(PODCAST_ID)).thenReturn(Optional.of(podcast));

        service.markFileUploaded(PODCAST_ID, "/media/audio.mp3", null);

        assertThat(podcast.getAudioUrlFile()).isEqualTo("/media/audio.mp3");
        assertThat(podcast.getAudioUrl()).isNull();
        assertThat(podcast.getAudioSizeFile()).isNull();
        assertThat(podcast.getDurationSeconds()).isNull();
        assertThat(podcast.getStatus()).isEqualTo(Status.UPLOADED);
        verify(podcastRepository).saveAndFlush(podcast);
    }

    @Test
    void processedStoresHlsAudioUrlDurationAndFileSizeThenMarksProcessed() {
        PodcastEntity podcast = podcast(Status.PROCESSING);
        when(podcastRepository.findDetailedByIdForUpdate(PODCAST_ID)).thenReturn(Optional.of(podcast));

        service.markProcessed(PODCAST_ID, "https://cdn.example.local/master.m3u8", 2400L, 11232332L, null);

        assertThat(podcast.getAudioUrl()).isEqualTo("https://cdn.example.local/master.m3u8");
        assertThat(podcast.getDurationSeconds()).isEqualTo(2400L);
        assertThat(podcast.getAudioSizeFile()).isEqualTo(11232332L);
        assertThat(podcast.getStatus()).isEqualTo(Status.PROCESSED);
        verify(podcastRepository).saveAndFlush(podcast);
    }

    @Test
    void processedRejectsMissingDurationSeconds() {
        PodcastEntity podcast = podcast(Status.PROCESSING);
        when(podcastRepository.findDetailedByIdForUpdate(PODCAST_ID)).thenReturn(Optional.of(podcast));

        assertThatThrownBy(() -> service.markProcessed(
                PODCAST_ID,
                "https://cdn.example.local/master.m3u8",
                null,
                11232332L,
                null
        ))
                .isInstanceOf(InvalidKafkaMessageException.class)
                .hasMessage("Received null duration_seconds in Kafka event");
    }

    @Test
    void processedRejectsMissingAudioFileSize() {
        PodcastEntity podcast = podcast(Status.PROCESSING);
        when(podcastRepository.findDetailedByIdForUpdate(PODCAST_ID)).thenReturn(Optional.of(podcast));

        assertThatThrownBy(() -> service.markProcessed(
                PODCAST_ID,
                "https://cdn.example.local/master.m3u8",
                2400L,
                null,
                null
        ))
                .isInstanceOf(InvalidKafkaMessageException.class)
                .hasMessage("Received null audio_file_size in Kafka event");
    }

    @Test
    void errorOnProcessingPodcastMarksFailed() {
        PodcastEntity podcast = podcast(Status.PROCESSING);
        when(podcastRepository.findDetailedByIdForUpdate(PODCAST_ID)).thenReturn(Optional.of(podcast));

        service.markFailed(PODCAST_ID, "processing failed", null, "media.worker");

        assertThat(podcast.getStatus()).isEqualTo(Status.FAILED);
        verify(podcastRepository).saveAndFlush(podcast);
    }

    @Test
    void subtitleEventStoresJsonContentInPodcastTranscriptContent() {
        PodcastEntity podcast = podcast(Status.PROCESSED);
        when(podcastRepository.findDetailedByIdForUpdate(PODCAST_ID)).thenReturn(Optional.of(podcast));
        when(podcastTranscriptRepository.findByIdPodcastIdAndIdLanguage(PODCAST_ID, "RU"))
                .thenReturn(Optional.empty());

        service.saveSubtitleContent(
                PODCAST_ID,
                "media/podcast/subtitles.vtt",
                "media/podcast/subtitles.srt",
                OffsetDateTime.parse("2026-03-22T12:35:56Z")
        );

        ArgumentCaptor<PodcastTranscriptEntity> captor = ArgumentCaptor.forClass(PodcastTranscriptEntity.class);
        verify(podcastTranscriptRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getContent())
                .contains("\"vtt_object_key\":\"media/podcast/subtitles.vtt\"")
                .contains("\"srt_object_key\":\"media/podcast/subtitles.srt\"")
                .contains("\"ready_at\"");
    }

    @Test
    void ttsEventStoresTextInPodcastTranscriptContent() {
        PodcastEntity podcast = podcast(Status.DRAFT);
        PodcastTranscriptEntity transcript = new PodcastTranscriptEntity();
        transcript.setId(new PodcastTranscriptId(PODCAST_ID, "RU"));
        transcript.setPodcast(podcast);
        when(podcastRepository.findDetailedByIdForUpdate(PODCAST_ID)).thenReturn(Optional.of(podcast));
        when(podcastTranscriptRepository.findByIdPodcastIdAndIdLanguage(PODCAST_ID, "RU"))
                .thenReturn(Optional.of(transcript));

        service.saveTtsContent(PODCAST_ID, "Текст для генерации аудио", OffsetDateTime.parse("2026-03-22T12:35:56Z"));

        assertThat(transcript.getContent()).isEqualTo("Текст для генерации аудио");
        verify(podcastTranscriptRepository).saveAndFlush(transcript);
    }

    @Test
    void ttsEventStoresTextAndMarksPodcastUploading() {
        PodcastEntity podcast = podcast(Status.DRAFT);
        PodcastTranscriptEntity transcript = new PodcastTranscriptEntity();
        transcript.setId(new PodcastTranscriptId(PODCAST_ID, "RU"));
        transcript.setPodcast(podcast);
        when(podcastRepository.findDetailedByIdForUpdate(PODCAST_ID)).thenReturn(Optional.of(podcast));
        when(podcastTranscriptRepository.findByIdPodcastIdAndIdLanguage(PODCAST_ID, "RU"))
                .thenReturn(Optional.of(transcript));

        service.saveTtsContentAndMarkUploading(
                PODCAST_ID,
                "Текст для генерации аудио",
                OffsetDateTime.parse("2026-03-22T12:35:56Z")
        );

        assertThat(transcript.getContent()).isEqualTo("Текст для генерации аудио");
        assertThat(podcast.getStatus()).isEqualTo(Status.UPLOADING);
        verify(podcastTranscriptRepository).saveAndFlush(transcript);
        verify(podcastRepository).saveAndFlush(podcast);
    }

    @Test
    void ttsEventStoresJsonArrayAndMarksPodcastUploading() throws Exception {
        PodcastEntity podcast = podcast(Status.DRAFT);
        PodcastTranscriptEntity transcript = new PodcastTranscriptEntity();
        transcript.setId(new PodcastTranscriptId(PODCAST_ID, "RU"));
        transcript.setPodcast(podcast);
        when(podcastRepository.findDetailedByIdForUpdate(PODCAST_ID)).thenReturn(Optional.of(podcast));
        when(podcastTranscriptRepository.findByIdPodcastIdAndIdLanguage(PODCAST_ID, "RU"))
                .thenReturn(Optional.of(transcript));

        service.saveTtsContentAndMarkUploading(
                PODCAST_ID,
                JsonMapper.builder().addModule(new JavaTimeModule()).build().readTree("""
                        [
                          {
                            "text": "Привет, как у тебя дела?",
                            "voice": "aidar"
                          },
                          {
                            "text": "У меня всё хорошо, а у тебя?",
                            "voice": "kseniya"
                          }
                        ]
                        """),
                OffsetDateTime.parse("2026-06-01T00:17:22Z")
        );

        assertThat(transcript.getContent())
                .contains("\"text\":\"Привет, как у тебя дела?\"")
                .contains("\"voice\":\"aidar\"")
                .contains("\"voice\":\"kseniya\"");
        assertThat(podcast.getStatus()).isEqualTo(Status.UPLOADING);
        verify(podcastTranscriptRepository).saveAndFlush(transcript);
        verify(podcastRepository).saveAndFlush(podcast);
    }

    private PodcastEntity podcast(Status status) {
        PodcastEntity podcast = new PodcastEntity();
        podcast.setId(PODCAST_ID);
        podcast.setStatus(status);
        return podcast;
    }
}
