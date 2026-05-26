package podcastService.podcast.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import podcastService.author.entity.AuthorEntity;
import podcastService.author.repository.AuthorRepository;
import podcastService.category.repository.CategoryRepository;
import podcastService.common.exception.NotFoundException;
import podcastService.common.exception.BusinessRuleException;
import podcastService.podcast.dto.CreatePodcastRequest;
import podcastService.podcast.dto.PodcastDetailResponse;
import podcastService.podcast.dto.PodcastSpeakersResponse;
import podcastService.podcast.entity.PodcastEntity;
import podcastService.podcast.entity.Status;
import podcastService.podcast.mapper.PodcastMapper;
import podcastService.podcast.repository.PodcastRepository;
import podcastService.podcast.repository.PodcastVoteRepository;
import podcastService.subscription.repository.SubscriptionRepository;
import podcastService.transcript.repository.PodcastSummaryRepository;
import podcastService.transcript.repository.PodcastTranscriptRepository;
import podcastService.user.entity.UserProfileEntity;
import podcastService.user.repository.UserProfileRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PodcastServiceTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID AUTHOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final UUID PODCAST_ID = UUID.fromString("00000000-0000-0000-0000-000000000301");

    @Mock private PodcastRepository podcastRepository;
    @Mock private AuthorRepository authorRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private PodcastVoteRepository podcastVoteRepository;
    @Mock private PodcastTranscriptRepository podcastTranscriptRepository;
    @Mock private PodcastSummaryRepository podcastSummaryRepository;

    private PodcastService service;

    @BeforeEach
    void setUp() {
        service = new PodcastService(
                podcastRepository,
                authorRepository,
                categoryRepository,
                new PodcastMapper(),
                userProfileRepository,
                subscriptionRepository,
                podcastVoteRepository,
                podcastTranscriptRepository,
                podcastSummaryRepository,
                new PodcastMediaStatusTransitionPolicy()
        );
    }

    @Test
    void createStoresNumSpeakersAndReturnsIt() {
        AuthorEntity author = author();
        when(authorRepository.findByUserProfileUserId(USER_ID)).thenReturn(Optional.of(author));
        when(podcastRepository.saveAndFlush(any(PodcastEntity.class))).thenAnswer(invocation -> {
            PodcastEntity podcast = invocation.getArgument(0);
            podcast.setId(PODCAST_ID);
            return podcast;
        });

        PodcastDetailResponse response = service.create(
                USER_ID,
                new CreatePodcastRequest("  Новый выпуск  ", "  Описание  ", null, null, 3)
        );

        ArgumentCaptor<PodcastEntity> captor = ArgumentCaptor.forClass(PodcastEntity.class);
        verify(podcastRepository).saveAndFlush(captor.capture());

        PodcastEntity saved = captor.getValue();
        assertThat(saved.getTitle()).isEqualTo("Новый выпуск");
        assertThat(saved.getDescription()).isEqualTo("Описание");
        assertThat(saved.getNumSpeakers()).isEqualTo(3);
        assertThat(response.numSpeakers()).isEqualTo(3);
    }

    @Test
    void getByIdReturnsNumSpeakersAndAudioFileFields() {
        PodcastEntity podcast = podcast(Status.PUBLISHED);
        podcast.setNumSpeakers(4);
        podcast.setAudioUrlFile("/media/podcasts/interview.mp3");
        podcast.setAudioSizeFile(78_000_000L);

        when(podcastRepository.findDetailedById(PODCAST_ID)).thenReturn(Optional.of(podcast));
        when(podcastTranscriptRepository.existsByIdPodcastIdAndContentIsNotNull(PODCAST_ID)).thenReturn(true);
        when(podcastSummaryRepository.existsByIdPodcastId(PODCAST_ID)).thenReturn(false);

        PodcastDetailResponse response = service.getById(PODCAST_ID, null);

        assertThat(response.id()).isEqualTo(PODCAST_ID);
        assertThat(response.numSpeakers()).isEqualTo(4);
        assertThat(response.audioUrlFile()).isEqualTo("/media/podcasts/interview.mp3");
        assertThat(response.audioSizeFile()).isEqualTo(78_000_000L);
        assertThat(response.hasTranscript()).isTrue();
        assertThat(response.hasSummary()).isFalse();
    }

    @Test
    void getByIdReturnsNotFoundForMissingPodcast() {
        when(podcastRepository.findDetailedById(PODCAST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(PODCAST_ID, null))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Podcast not found");
    }

    @Test
    void getSpeakersByIdReturnsNumSpeakersForPublishedPodcast() {
        PodcastEntity podcast = podcast(Status.PUBLISHED);
        podcast.setNumSpeakers(5);

        when(podcastRepository.findDetailedById(PODCAST_ID)).thenReturn(Optional.of(podcast));

        PodcastSpeakersResponse response = service.getSpeakersById(PODCAST_ID, null);

        assertThat(response.podcastId()).isEqualTo(PODCAST_ID);
        assertThat(response.numSpeakers()).isEqualTo(5);
    }

    @Test
    void getSpeakersByIdReturnsNumSpeakersForDraftPodcastWithoutUser() {
        PodcastEntity podcast = podcast(Status.DRAFT);
        podcast.setNumSpeakers(3);

        when(podcastRepository.findDetailedById(PODCAST_ID)).thenReturn(Optional.of(podcast));

        PodcastSpeakersResponse response = service.getSpeakersById(PODCAST_ID, null);

        assertThat(response.podcastId()).isEqualTo(PODCAST_ID);
        assertThat(response.numSpeakers()).isEqualTo(3);
    }

    @Test
    void publishRejectsPodcastBeforeMediaIsProcessed() {
        PodcastEntity podcast = podcast(Status.UPLOADED);
        when(podcastRepository.findDetailedByIdForUpdate(PODCAST_ID)).thenReturn(Optional.of(podcast));
        when(authorRepository.findByUserProfileUserId(USER_ID)).thenReturn(Optional.of(author()));

        assertThatThrownBy(() -> service.publish(PODCAST_ID, USER_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Cannot publish a podcast before media is processed");
    }

    @Test
    void publishRejectsProcessedPodcastWithoutAudioUrl() {
        PodcastEntity podcast = podcast(Status.PROCESSED);
        when(podcastRepository.findDetailedByIdForUpdate(PODCAST_ID)).thenReturn(Optional.of(podcast));
        when(authorRepository.findByUserProfileUserId(USER_ID)).thenReturn(Optional.of(author()));

        assertThatThrownBy(() -> service.publish(PODCAST_ID, USER_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Cannot publish a podcast without processed audio_url");
    }

    @Test
    void publishAllowsProcessedPodcastWithAudioUrl() {
        PodcastEntity podcast = podcast(Status.PROCESSED);
        podcast.setAudioUrl("https://cdn.example.local/hls/podcast/master.m3u8");
        podcast.setDurationSeconds(2400L);
        when(podcastRepository.findDetailedByIdForUpdate(PODCAST_ID)).thenReturn(Optional.of(podcast));
        when(authorRepository.findByUserProfileUserId(USER_ID)).thenReturn(Optional.of(author()));
        when(podcastRepository.saveAndFlush(podcast)).thenReturn(podcast);
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(author().getUserProfile()));
        when(subscriptionRepository.findSubscribedAuthorIds(any(), any())).thenReturn(java.util.Set.of());
        when(podcastTranscriptRepository.existsByIdPodcastIdAndContentIsNotNull(PODCAST_ID)).thenReturn(false);
        when(podcastSummaryRepository.existsByIdPodcastId(PODCAST_ID)).thenReturn(false);

        PodcastDetailResponse response = service.publish(PODCAST_ID, USER_ID);

        assertThat(response.status()).isEqualTo(Status.PUBLISHED);
        assertThat(podcast.getStatus()).isEqualTo(Status.PUBLISHED);
    }

    @Test
    void publishRejectsProcessedPodcastWithoutDuration() {
        PodcastEntity podcast = podcast(Status.PROCESSED);
        podcast.setAudioUrl("https://cdn.example.local/hls/podcast/master.m3u8");
        when(podcastRepository.findDetailedByIdForUpdate(PODCAST_ID)).thenReturn(Optional.of(podcast));
        when(authorRepository.findByUserProfileUserId(USER_ID)).thenReturn(Optional.of(author()));

        assertThatThrownBy(() -> service.publish(PODCAST_ID, USER_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Cannot publish a podcast without positive duration_seconds");
    }

    private AuthorEntity author() {
        UserProfileEntity profile = new UserProfileEntity();
        profile.setId(PROFILE_ID);
        profile.setUserId(USER_ID);
        profile.setUsername("dev-user");

        AuthorEntity author = new AuthorEntity();
        author.setId(AUTHOR_ID);
        author.setUserProfile(profile);
        author.setAuthorName("Dev Author");
        return author;
    }

    private PodcastEntity podcast(Status status) {
        PodcastEntity podcast = new PodcastEntity();
        podcast.setId(PODCAST_ID);
        podcast.setAuthor(author());
        podcast.setTitle("Интервью");
        podcast.setStatus(status);
        podcast.setNumSpeakers(2);
        return podcast;
    }
}
