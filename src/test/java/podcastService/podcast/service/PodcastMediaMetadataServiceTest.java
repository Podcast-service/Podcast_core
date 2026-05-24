package podcastService.podcast.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import podcastService.podcast.entity.PodcastEntity;
import podcastService.podcast.entity.Status;
import podcastService.podcast.repository.PodcastRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PodcastMediaMetadataServiceTest {

    private static final UUID PODCAST_ID = UUID.fromString("00000000-0000-0000-0000-000000000301");

    @Mock
    private PodcastRepository podcastRepository;

    private PodcastMediaMetadataService service;

    @BeforeEach
    void setUp() {
        service = new PodcastMediaMetadataService(podcastRepository);
    }

    @Test
    void startUploadMovesDraftToProcessing() {
        PodcastEntity podcast = podcast(Status.DRAFT);
        when(podcastRepository.findDetailedByIdForUpdate(PODCAST_ID)).thenReturn(Optional.of(podcast));

        service.markFileUploadStarted(PODCAST_ID);

        assertThat(podcast.getStatus()).isEqualTo(Status.PROCESSING);
        verify(podcastRepository).saveAndFlush(podcast);
    }

    @Test
    void startUploadDoesNotDowngradeReadyToPublish() {
        PodcastEntity podcast = podcast(Status.READY_TO_PUBLISH);
        when(podcastRepository.findDetailedByIdForUpdate(PODCAST_ID)).thenReturn(Optional.of(podcast));

        service.markFileUploadStarted(PODCAST_ID);

        assertThat(podcast.getStatus()).isEqualTo(Status.READY_TO_PUBLISH);
        verify(podcastRepository, never()).saveAndFlush(any(PodcastEntity.class));
    }

    @Test
    void uploadedStoresAudioMetadataAndMarksReadyToPublish() {
        PodcastEntity podcast = podcast(Status.PROCESSING);
        when(podcastRepository.findDetailedByIdForUpdate(PODCAST_ID)).thenReturn(Optional.of(podcast));

        service.markFileUploaded(PODCAST_ID, "/media/audio.mp3", 123L);

        assertThat(podcast.getAudioUrlFile()).isEqualTo("/media/audio.mp3");
        assertThat(podcast.getAudioUrl()).isEqualTo("/media/audio.mp3");
        assertThat(podcast.getAudioSizeFile()).isEqualTo(123L);
        assertThat(podcast.getStatus()).isEqualTo(Status.READY_TO_PUBLISH);
        verify(podcastRepository).saveAndFlush(podcast);
    }

    @Test
    void errorAfterReadyToPublishDoesNotDowngradeStatus() {
        PodcastEntity podcast = podcast(Status.READY_TO_PUBLISH);
        when(podcastRepository.findDetailedByIdForUpdate(PODCAST_ID)).thenReturn(Optional.of(podcast));

        service.markFileUploadError(PODCAST_ID, "late error");

        assertThat(podcast.getStatus()).isEqualTo(Status.READY_TO_PUBLISH);
        verify(podcastRepository, never()).saveAndFlush(any(PodcastEntity.class));
    }

    private PodcastEntity podcast(Status status) {
        PodcastEntity podcast = new PodcastEntity();
        podcast.setId(PODCAST_ID);
        podcast.setStatus(status);
        return podcast;
    }
}
