package podcastService.podcast.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import podcastService.author.entity.AuthorEntity;
import podcastService.category.entity.CategoryEntity;
import podcastService.common.exception.NotFoundException;
import podcastService.infrastructure.config.JacksonConfig;
import podcastService.infrastructure.outbox.OutboxEventService;
import podcastService.infrastructure.outbox.RecommendationEventsProperties;
import podcastService.infrastructure.outbox.entity.OutboxEventEntity;
import podcastService.infrastructure.outbox.repository.OutboxEventRepository;
import podcastService.infrastructure.outbox.recommendation.RecommendationEventTypes;
import podcastService.infrastructure.outbox.recommendation.RecommendationOutboxEventService;
import podcastService.podcast.entity.PodcastEntity;
import podcastService.podcast.entity.PodcastVoteEntity;
import podcastService.podcast.entity.PodcastVoteId;
import podcastService.podcast.entity.Status;
import podcastService.podcast.repository.PodcastRepository;
import podcastService.podcast.repository.PodcastVoteRepository;
import podcastService.user.entity.UserProfileEntity;
import podcastService.user.repository.UserProfileRepository;
import podcastService.vote.dto.VoteRequest;
import podcastService.vote.dto.VoteResponse;
import podcastService.vote.dto.VoteType;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PodcastVoteServiceTest {

    private static final UUID USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID PROFILE_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final UUID PODCAST_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");
    private static final UUID AUTHOR_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440020");
    private static final UUID CATEGORY_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440030");

    @Mock
    private PodcastRepository podcastRepository;

    @Mock
    private PodcastVoteRepository podcastVoteRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private PodcastVoteService service;

    @BeforeEach
    void setUp() {
        service = serviceWithRecommendationEvents(false);
    }

    @Test
    void voteCreatesNewLikeAndUpdatesCounters() {
        PodcastEntity podcast = publishedPodcast();
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(userProfile()));
        when(podcastRepository.findDetailedByIdForUpdate(PODCAST_ID)).thenReturn(Optional.of(podcast));
        when(podcastVoteRepository.findByIdUserProfileIdAndIdPodcastId(PROFILE_ID, PODCAST_ID))
                .thenReturn(Optional.empty());
        when(podcastRepository.saveAndFlush(podcast)).thenReturn(podcast);

        VoteResponse response = service.vote(PODCAST_ID, USER_ID, new VoteRequest(VoteType.LIKE));

        assertThat(podcast.getLikesCount()).isEqualTo(1);
        assertThat(podcast.getDislikesCount()).isZero();
        assertThat(response.targetType()).isEqualTo("PODCAST");
        assertThat(response.currentUserVote()).isEqualTo(VoteType.LIKE);
        verify(podcastVoteRepository).save(any(PodcastVoteEntity.class));
        verifyNoInteractions(outboxEventRepository);
    }

    @Test
    void voteSwitchesExistingVote() {
        PodcastEntity podcast = publishedPodcast();
        podcast.setLikesCount(1);
        PodcastVoteEntity existingVote = vote(podcast, VoteType.LIKE);

        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(userProfile()));
        when(podcastRepository.findDetailedByIdForUpdate(PODCAST_ID)).thenReturn(Optional.of(podcast));
        when(podcastVoteRepository.findByIdUserProfileIdAndIdPodcastId(PROFILE_ID, PODCAST_ID))
                .thenReturn(Optional.of(existingVote));
        when(podcastRepository.saveAndFlush(podcast)).thenReturn(podcast);

        VoteResponse response = service.vote(PODCAST_ID, USER_ID, new VoteRequest(VoteType.DISLIKE));

        assertThat(podcast.getLikesCount()).isZero();
        assertThat(podcast.getDislikesCount()).isEqualTo(1);
        assertThat(existingVote.getVoteType()).isEqualTo(VoteType.DISLIKE);
        assertThat(response.currentUserVote()).isEqualTo(VoteType.DISLIKE);
        verify(podcastVoteRepository, never()).save(any(PodcastVoteEntity.class));
        verifyNoInteractions(outboxEventRepository);
    }

    @Test
    void voteCreatesPodcastLikedOutboxEventWhenRecommendationEventsEnabled() {
        PodcastEntity podcast = publishedPodcast();
        service = serviceWithRecommendationEvents(true);

        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(userProfile()));
        when(podcastRepository.findDetailedByIdForUpdate(PODCAST_ID)).thenReturn(Optional.of(podcast));
        when(podcastVoteRepository.findByIdUserProfileIdAndIdPodcastId(PROFILE_ID, PODCAST_ID))
                .thenReturn(Optional.empty());
        when(podcastRepository.saveAndFlush(podcast)).thenReturn(podcast);
        when(outboxEventRepository.saveAndFlush(any(OutboxEventEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VoteResponse response = service.vote(PODCAST_ID, USER_ID, new VoteRequest(VoteType.LIKE));

        org.mockito.ArgumentCaptor<OutboxEventEntity> captor =
                org.mockito.ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(outboxEventRepository).saveAndFlush(captor.capture());

        OutboxEventEntity outboxEvent = captor.getValue();
        JsonNode payload = outboxEvent.getPayload();
        assertThat(response.currentUserVote()).isEqualTo(VoteType.LIKE);
        assertThat(outboxEvent.getEventType()).isEqualTo(RecommendationEventTypes.PODCAST_LIKED);
        assertThat(outboxEvent.getEventVersion()).isEqualTo(1);
        assertThat(outboxEvent.getEventKey()).isEqualTo(USER_ID.toString());
        assertThat(outboxEvent.getStatus().name()).isEqualTo("NEW");
        assertThat(outboxEvent.getAggregateType()).isEqualTo("USER_ACTIVITY");
        assertThat(outboxEvent.getAggregateId()).isEqualTo(USER_ID);
        assertThat(payload.get("eventType").asText()).isEqualTo(RecommendationEventTypes.PODCAST_LIKED);
        assertThat(payload.get("eventVersion").asInt()).isEqualTo(1);
        assertThat(payload.get("producer").asText()).isEqualTo("podcast-core");
        assertThat(payload.get("userId").asText()).isEqualTo(USER_ID.toString());
        assertThat(payload.get("payload").get("podcastId").asText()).isEqualTo(PODCAST_ID.toString());
        assertThat(payload.get("payload").get("userId").asText()).isEqualTo(USER_ID.toString());
        assertThat(payload.get("payload").get("authorId").asText()).isEqualTo(AUTHOR_ID.toString());
        assertThat(payload.get("payload").get("categoryId").asText()).isEqualTo(CATEGORY_ID.toString());
    }

    @Test
    void voteCreatesPodcastDislikedOutboxEventWhenRecommendationEventsEnabled() {
        PodcastEntity podcast = publishedPodcast();
        service = serviceWithRecommendationEvents(true);

        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(userProfile()));
        when(podcastRepository.findDetailedByIdForUpdate(PODCAST_ID)).thenReturn(Optional.of(podcast));
        when(podcastVoteRepository.findByIdUserProfileIdAndIdPodcastId(PROFILE_ID, PODCAST_ID))
                .thenReturn(Optional.empty());
        when(podcastRepository.saveAndFlush(podcast)).thenReturn(podcast);
        when(outboxEventRepository.saveAndFlush(any(OutboxEventEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VoteResponse response = service.vote(PODCAST_ID, USER_ID, new VoteRequest(VoteType.DISLIKE));

        org.mockito.ArgumentCaptor<OutboxEventEntity> captor =
                org.mockito.ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(outboxEventRepository).saveAndFlush(captor.capture());

        OutboxEventEntity outboxEvent = captor.getValue();
        assertThat(response.currentUserVote()).isEqualTo(VoteType.DISLIKE);
        assertThat(outboxEvent.getEventType()).isEqualTo(RecommendationEventTypes.PODCAST_DISLIKED);
        assertThat(outboxEvent.getEventVersion()).isEqualTo(1);
        assertThat(outboxEvent.getEventKey()).isEqualTo(USER_ID.toString());
        assertThat(outboxEvent.getAggregateType()).isEqualTo("USER_ACTIVITY");
        assertThat(outboxEvent.getAggregateId()).isEqualTo(USER_ID);
        assertThat(outboxEvent.getPayload().get("payload").get("podcastId").asText()).isEqualTo(PODCAST_ID.toString());
        assertThat(outboxEvent.getPayload().get("payload").get("userId").asText()).isEqualTo(USER_ID.toString());
    }

    @Test
    void voteDoesNotCreateOutboxEventWhenBusinessValidationFails() {
        PodcastEntity podcast = publishedPodcast();
        podcast.setStatus(Status.DRAFT);

        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(userProfile()));
        when(podcastRepository.findDetailedByIdForUpdate(PODCAST_ID)).thenReturn(Optional.of(podcast));

        assertThatThrownBy(() -> service.vote(PODCAST_ID, USER_ID, new VoteRequest(VoteType.LIKE)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Podcast not found");

        verifyNoInteractions(outboxEventRepository);
        verify(podcastRepository, never()).saveAndFlush(any());
        verify(podcastVoteRepository, never()).save(any());
    }

    @Test
    void removeVoteIsIdempotentWhenVoteDoesNotExist() {
        PodcastEntity podcast = publishedPodcast();
        podcast.setLikesCount(3);
        podcast.setDislikesCount(1);

        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(userProfile()));
        when(podcastRepository.findDetailedByIdForUpdate(PODCAST_ID)).thenReturn(Optional.of(podcast));
        when(podcastVoteRepository.findByIdUserProfileIdAndIdPodcastId(PROFILE_ID, PODCAST_ID))
                .thenReturn(Optional.empty());

        VoteResponse response = service.removeVote(PODCAST_ID, USER_ID);

        assertThat(response.likesCount()).isEqualTo(3);
        assertThat(response.dislikesCount()).isEqualTo(1);
        assertThat(response.currentUserVote()).isNull();
        verify(podcastVoteRepository, never()).delete(any());
    }

    @Test
    void voteDoesNotExposeUnpublishedPodcast() {
        PodcastEntity podcast = publishedPodcast();
        podcast.setStatus(Status.DRAFT);

        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(userProfile()));
        when(podcastRepository.findDetailedByIdForUpdate(PODCAST_ID)).thenReturn(Optional.of(podcast));

        assertThatThrownBy(() -> service.vote(PODCAST_ID, USER_ID, new VoteRequest(VoteType.LIKE)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Podcast not found");

        verify(podcastVoteRepository, never()).save(any());
    }

    private UserProfileEntity userProfile() {
        UserProfileEntity user = new UserProfileEntity();
        user.setId(PROFILE_ID);
        user.setUserId(USER_ID);
        user.setUsername("listener");
        return user;
    }

    private PodcastEntity publishedPodcast() {
        PodcastEntity podcast = new PodcastEntity();
        podcast.setId(PODCAST_ID);
        podcast.setStatus(Status.PUBLISHED);
        AuthorEntity author = new AuthorEntity();
        author.setId(AUTHOR_ID);
        podcast.setAuthor(author);
        CategoryEntity category = new CategoryEntity();
        category.setId(CATEGORY_ID);
        podcast.setCategory(category);
        return podcast;
    }

    private PodcastVoteEntity vote(PodcastEntity podcast, VoteType voteType) {
        PodcastVoteEntity vote = new PodcastVoteEntity();
        vote.setId(new PodcastVoteId(PROFILE_ID, PODCAST_ID));
        vote.setPodcast(podcast);
        vote.setUserProfile(userProfile());
        vote.setVoteType(voteType);
        return vote;
    }

    private PodcastVoteService serviceWithRecommendationEvents(boolean enabled) {
        OutboxEventService outboxEventService = new OutboxEventService(
                outboxEventRepository,
                new JacksonConfig().objectMapper()
        );
        RecommendationOutboxEventService recommendationOutboxEventService = new RecommendationOutboxEventService(
                outboxEventService,
                new RecommendationEventsProperties(enabled)
        );
        return new PodcastVoteService(
                podcastRepository,
                podcastVoteRepository,
                userProfileRepository,
                recommendationOutboxEventService
        );
    }
}
