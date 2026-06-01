package podcastService.history.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import podcastService.author.entity.AuthorEntity;
import podcastService.author.repository.AuthorRepository;
import podcastService.category.entity.CategoryEntity;
import podcastService.history.dto.SaveProgressRequest;
import podcastService.history.repository.ListenHistoryRepository;
import podcastService.infrastructure.config.JacksonConfig;
import podcastService.infrastructure.outbox.OutboxEventService;
import podcastService.infrastructure.outbox.RecommendationEventsProperties;
import podcastService.infrastructure.outbox.entity.OutboxEventEntity;
import podcastService.infrastructure.outbox.recommendation.RecommendationEventTypes;
import podcastService.infrastructure.outbox.recommendation.RecommendationOutboxEventService;
import podcastService.infrastructure.outbox.repository.OutboxEventRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListenHistoryServiceTest {

    private static final UUID USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID PROFILE_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final UUID PODCAST_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");
    private static final UUID AUTHOR_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440020");
    private static final UUID CATEGORY_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440030");

    @Mock private ListenHistoryRepository listenHistoryRepository;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private PodcastRepository podcastRepository;
    @Mock private PodcastVoteRepository podcastVoteRepository;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private AuthorRepository authorRepository;
    @Mock private PodcastMapper podcastMapper;
    @Mock private OutboxEventRepository outboxEventRepository;

    private ListenHistoryService service;

    @BeforeEach
    void setUp() {
        service = serviceWithRecommendationEvents(false);
    }

    @Test
    void saveProgressCreatesHistoryAndMarksCompletedAtThreshold() {
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(userProfile()));
        when(podcastRepository.findDetailedById(PODCAST_ID)).thenReturn(Optional.of(podcast(100L)));
        when(listenHistoryRepository.findByIdUserProfileIdAndIdPodcastId(PROFILE_ID, PODCAST_ID)).thenReturn(Optional.empty());
        service.saveProgress(PODCAST_ID, USER_ID, new SaveProgressRequest(95));

        verify(listenHistoryRepository).upsertProgress(PROFILE_ID, PODCAST_ID, 95, true);
        verify(listenHistoryRepository, never()).saveAndFlush(any());
        verifyNoInteractions(outboxEventRepository);
    }

    @Test
    void saveProgressClampsProgressToDuration() {
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(userProfile()));
        when(podcastRepository.findDetailedById(PODCAST_ID)).thenReturn(Optional.of(podcast(100L)));
        when(listenHistoryRepository.findByIdUserProfileIdAndIdPodcastId(PROFILE_ID, PODCAST_ID)).thenReturn(Optional.empty());
        service.saveProgress(PODCAST_ID, USER_ID, new SaveProgressRequest(150));

        verify(listenHistoryRepository).upsertProgress(PROFILE_ID, PODCAST_ID, 100, true);
    }

    @Test
    void saveProgressCreatesPlayFinishedOutboxEventWhenRecommendationEventsEnabled() {
        service = serviceWithRecommendationEvents(true);
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(userProfile()));
        when(podcastRepository.findDetailedById(PODCAST_ID)).thenReturn(Optional.of(podcast(100L)));
        when(listenHistoryRepository.findByIdUserProfileIdAndIdPodcastId(PROFILE_ID, PODCAST_ID)).thenReturn(Optional.empty());
        when(outboxEventRepository.saveAndFlush(any(OutboxEventEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.saveProgress(PODCAST_ID, USER_ID, new SaveProgressRequest(95));

        org.mockito.ArgumentCaptor<OutboxEventEntity> captor =
                org.mockito.ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(outboxEventRepository).saveAndFlush(captor.capture());
        OutboxEventEntity outboxEvent = captor.getValue();

        verify(listenHistoryRepository).upsertProgress(PROFILE_ID, PODCAST_ID, 95, true);
        assertThat(outboxEvent.getAggregateType()).isEqualTo("USER_ACTIVITY");
        assertThat(outboxEvent.getAggregateId()).isEqualTo(USER_ID);
        assertThat(outboxEvent.getEventKey()).isEqualTo(USER_ID.toString());
        assertThat(outboxEvent.getEventType()).isEqualTo(RecommendationEventTypes.PODCAST_PLAY_FINISHED);
        assertThat(outboxEvent.getEventVersion()).isEqualTo(1);
        assertThat(outboxEvent.getPayload().get("payload").get("podcastId").asText()).isEqualTo(PODCAST_ID.toString());
        assertThat(outboxEvent.getPayload().get("payload").get("userId").asText()).isEqualTo(USER_ID.toString());
        assertThat(outboxEvent.getPayload().get("payload").get("progressSeconds").asLong()).isEqualTo(95L);
        assertThat(outboxEvent.getPayload().get("payload").get("authorId").asText()).isEqualTo(AUTHOR_ID.toString());
        assertThat(outboxEvent.getPayload().get("payload").get("categoryId").asText()).isEqualTo(CATEGORY_ID.toString());
        assertThat(outboxEvent.getPayload().get("payload").get("progressPercent").decimalValue()).isEqualByComparingTo("95.00");
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
        AuthorEntity author = new AuthorEntity();
        author.setId(AUTHOR_ID);
        podcast.setAuthor(author);
        CategoryEntity category = new CategoryEntity();
        category.setId(CATEGORY_ID);
        podcast.setCategory(category);
        return podcast;
    }

    private ListenHistoryService serviceWithRecommendationEvents(boolean enabled) {
        OutboxEventService outboxEventService = new OutboxEventService(
                outboxEventRepository,
                new JacksonConfig().objectMapper()
        );
        RecommendationOutboxEventService recommendationOutboxEventService = new RecommendationOutboxEventService(
                outboxEventService,
                new RecommendationEventsProperties(enabled)
        );
        return new ListenHistoryService(
                listenHistoryRepository,
                userProfileRepository,
                podcastRepository,
                podcastVoteRepository,
                subscriptionRepository,
                authorRepository,
                podcastMapper,
                recommendationOutboxEventService
        );
    }
}
