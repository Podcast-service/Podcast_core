package podcastService.history.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import podcastService.author.repository.AuthorRepository;
import podcastService.history.dto.SaveProgressRequest;
import podcastService.history.repository.ListenHistoryRepository;
import podcastService.podcast.entity.PodcastEntity;
import podcastService.podcast.entity.Status;
import podcastService.podcast.mapper.PodcastMapper;
import podcastService.podcast.repository.PodcastRepository;
import podcastService.podcast.repository.PodcastVoteRepository;
import podcastService.subscription.repository.SubscriptionRepository;
import podcastService.user.entity.UserProfileEntity;
import podcastService.user.repository.UserProfileRepository;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListenHistoryServiceTest {

    private static final UUID USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID PROFILE_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final UUID PODCAST_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");

    @Mock private ListenHistoryRepository listenHistoryRepository;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private PodcastRepository podcastRepository;
    @Mock private PodcastVoteRepository podcastVoteRepository;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private AuthorRepository authorRepository;
    @Mock private PodcastMapper podcastMapper;

    private ListenHistoryService service;

    @BeforeEach
    void setUp() {
        service = new ListenHistoryService(
                listenHistoryRepository,
                userProfileRepository,
                podcastRepository,
                podcastVoteRepository,
                subscriptionRepository,
                authorRepository,
                podcastMapper
        );
    }

    @Test
    void saveProgressCreatesHistoryAndMarksCompletedAtThreshold() {
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(userProfile()));
        when(podcastRepository.findDetailedById(PODCAST_ID)).thenReturn(Optional.of(podcast(100L)));
        service.saveProgress(PODCAST_ID, USER_ID, new SaveProgressRequest(95));

        verify(listenHistoryRepository).upsertProgress(PROFILE_ID, PODCAST_ID, 95, true);
        verify(listenHistoryRepository, never()).saveAndFlush(any());
    }

    @Test
    void saveProgressClampsProgressToDuration() {
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(userProfile()));
        when(podcastRepository.findDetailedById(PODCAST_ID)).thenReturn(Optional.of(podcast(100L)));
        service.saveProgress(PODCAST_ID, USER_ID, new SaveProgressRequest(150));

        verify(listenHistoryRepository).upsertProgress(PROFILE_ID, PODCAST_ID, 100, true);
    }

    private UserProfileEntity userProfile() {
        UserProfileEntity user = new UserProfileEntity();
        user.setId(PROFILE_ID);
        user.setUserId(USER_ID);
        user.setUsername("listener");
        return user;
    }

    private PodcastEntity podcast(Long durationSeconds) {
        PodcastEntity podcast = new PodcastEntity();
        podcast.setId(PODCAST_ID);
        podcast.setStatus(Status.PUBLISHED);
        podcast.setDurationSeconds(durationSeconds);
        return podcast;
    }
}
