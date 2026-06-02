package podcastService.playlist.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import podcastService.author.repository.AuthorRepository;
import podcastService.infrastructure.config.JacksonConfig;
import podcastService.infrastructure.outbox.OutboxEventService;
import podcastService.infrastructure.outbox.RecommendationEventsProperties;
import podcastService.infrastructure.outbox.entity.OutboxEventEntity;
import podcastService.infrastructure.outbox.recommendation.RecommendationEventTypes;
import podcastService.infrastructure.outbox.recommendation.RecommendationOutboxEventService;
import podcastService.infrastructure.outbox.repository.OutboxEventRepository;
import podcastService.playlist.dto.AddPodcastToPlaylistRequest;
import podcastService.playlist.dto.PlaylistDetailResponse;
import podcastService.playlist.dto.PlaylistOwnerResponse;
import podcastService.playlist.dto.ReorderPlaylistRequest;
import podcastService.playlist.dto.UpdatePlaylistRequest;
import podcastService.playlist.entity.PlaylistEntity;
import podcastService.playlist.entity.PlaylistPodcastEntity;
import podcastService.playlist.entity.PlaylistPodcastId;
import podcastService.playlist.mapper.PlaylistMapper;
import podcastService.playlist.repository.PlaylistPodcastRepository;
import podcastService.playlist.repository.PlaylistRepository;
import podcastService.playlist.repository.PlaylistVoteRepository;
import podcastService.playlist.repository.SavedPlaylistRepository;
import podcastService.podcast.entity.PodcastEntity;
import podcastService.podcast.entity.Status;
import podcastService.podcast.repository.PodcastRepository;
import podcastService.user.entity.UserProfileEntity;
import podcastService.user.repository.UserProfileRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaylistServiceMutationTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID USER_PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID PLAYLIST_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final UUID PODCAST_ONE_ID = UUID.fromString("00000000-0000-0000-0000-000000000301");
    private static final UUID PODCAST_TWO_ID = UUID.fromString("00000000-0000-0000-0000-000000000302");

    @Mock private PlaylistRepository playlistRepository;
    @Mock private PlaylistPodcastRepository playlistPodcastRepository;
    @Mock private PlaylistVoteRepository playlistVoteRepository;
    @Mock private SavedPlaylistRepository savedPlaylistRepository;
    @Mock private PodcastRepository podcastRepository;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private AuthorRepository authorRepository;
    @Mock private PlaylistMapper playlistMapper;
    @Mock private EntityManager entityManager;
    @Mock private OutboxEventRepository outboxEventRepository;

    private PlaylistService service;

    @BeforeEach
    void setUp() {
        service = serviceWithRecommendationEvents(false);
    }

    @Test
    void updatePlaylistMutatesOnlyProvidedFieldsAndReturnsFreshDetail() {
        PlaylistEntity playlist = playlist();
        UpdatePlaylistRequest request = new UpdatePlaylistRequest();
        request.setDescription("  Новое описание  ");
        request.setIsPublic(false);
        stubOwnerMutation(playlist);
        stubDetailAfterMutation(playlist);

        PlaylistDetailResponse response = service.update(PLAYLIST_ID, USER_ID, request);

        assertThat(response.id()).isEqualTo(PLAYLIST_ID);
        assertThat(playlist.getDescription()).isEqualTo("Новое описание");
        assertThat(playlist.isPublicPlaylist()).isFalse();
        assertThat(playlist.getTitle()).isEqualTo("Original title");
        verify(playlistRepository).saveAndFlush(playlist);
        verifyNoInteractions(outboxEventRepository);
    }

    @Test
    void updatePlaylistCreatesOutboxEventWhenRecommendationEventsEnabled() {
        service = serviceWithRecommendationEvents(true);
        PlaylistEntity playlist = playlist();
        UpdatePlaylistRequest request = new UpdatePlaylistRequest();
        request.setTitle("  Новое название  ");
        request.setIsPublic(false);
        stubOwnerMutation(playlist);
        stubDetailAfterMutation(playlist);
        when(outboxEventRepository.saveAndFlush(any(OutboxEventEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PlaylistDetailResponse response = service.update(PLAYLIST_ID, USER_ID, request);

        ArgumentCaptor<OutboxEventEntity> captor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(outboxEventRepository).saveAndFlush(captor.capture());
        OutboxEventEntity outboxEvent = captor.getValue();

        assertThat(response.id()).isEqualTo(PLAYLIST_ID);
        assertThat(playlist.getTitle()).isEqualTo("Новое название");
        assertThat(outboxEvent.getAggregateType()).isEqualTo("PLAYLIST");
        assertThat(outboxEvent.getAggregateId()).isEqualTo(PLAYLIST_ID);
        assertThat(outboxEvent.getEventKey()).isEqualTo(PLAYLIST_ID.toString());
        assertThat(outboxEvent.getEventType()).isEqualTo(RecommendationEventTypes.PLAYLIST_UPDATED);
        assertThat(outboxEvent.getEventVersion()).isEqualTo(1);
        assertThat(outboxEvent.getPayload().get("payload").get("playlistId").asText()).isEqualTo(PLAYLIST_ID.toString());
        assertThat(outboxEvent.getPayload().get("payload").get("ownerUserId").asText()).isEqualTo(USER_ID.toString());
        assertThat(outboxEvent.getPayload().get("payload").get("title").asText()).isEqualTo("Новое название");
    }

    @Test
    void addPodcastAppendsPublishedPodcastAtNextPosition() {
        PlaylistEntity playlist = playlist();
        PodcastEntity podcast = podcast(PODCAST_ONE_ID, Status.PUBLISHED);
        stubOwnerMutation(playlist);
        stubDetailAfterMutation(playlist);
        when(podcastRepository.findDetailedById(PODCAST_ONE_ID)).thenReturn(Optional.of(podcast));
        when(playlistPodcastRepository.existsByIdPlaylistIdAndIdPodcastId(PLAYLIST_ID, PODCAST_ONE_ID))
                .thenReturn(false);
        when(playlistPodcastRepository.findMaxPosition(PLAYLIST_ID)).thenReturn(2);
        when(playlistPodcastRepository.saveAndFlush(any(PlaylistPodcastEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.addPodcast(PLAYLIST_ID, USER_ID, new AddPodcastToPlaylistRequest(PODCAST_ONE_ID));

        ArgumentCaptor<PlaylistPodcastEntity> captor = ArgumentCaptor.forClass(PlaylistPodcastEntity.class);
        verify(playlistPodcastRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getId().getPlaylistId()).isEqualTo(PLAYLIST_ID);
        assertThat(captor.getValue().getId().getPodcastId()).isEqualTo(PODCAST_ONE_ID);
        assertThat(captor.getValue().getPosition()).isEqualTo(3);
    }

    @Test
    void addPodcastCreatesPlaylistSnapshotEventWhenRecommendationEventsEnabled() {
        service = serviceWithRecommendationEvents(true);
        PlaylistEntity playlist = playlist();
        PodcastEntity podcast = podcast(PODCAST_ONE_ID, Status.PUBLISHED);
        stubOwnerMutation(playlist);
        stubDetailAfterMutation(playlist);
        when(podcastRepository.findDetailedById(PODCAST_ONE_ID)).thenReturn(Optional.of(podcast));
        when(playlistPodcastRepository.existsByIdPlaylistIdAndIdPodcastId(PLAYLIST_ID, PODCAST_ONE_ID))
                .thenReturn(false);
        when(playlistPodcastRepository.findMaxPosition(PLAYLIST_ID)).thenReturn(0);
        when(playlistPodcastRepository.saveAndFlush(any(PlaylistPodcastEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(playlistPodcastRepository.findPodcastIdsByPlaylistIdOrderByPositionAsc(PLAYLIST_ID))
                .thenReturn(List.of(PODCAST_ONE_ID));
        when(outboxEventRepository.saveAndFlush(any(OutboxEventEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.addPodcast(PLAYLIST_ID, USER_ID, new AddPodcastToPlaylistRequest(PODCAST_ONE_ID));

        ArgumentCaptor<OutboxEventEntity> captor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(outboxEventRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo(RecommendationEventTypes.PLAYLIST_UPDATED);
        assertThat(captor.getValue().getPayload().get("payload").get("podcastIds").get(0).asText())
                .isEqualTo(PODCAST_ONE_ID.toString());
    }

    @Test
    void reorderOffsetsPositionsBeforeApplyingRequestedOrder() {
        PlaylistEntity playlist = playlist();
        List<PlaylistPodcastEntity> existingItems = List.of(
                playlistItem(PODCAST_ONE_ID, 1),
                playlistItem(PODCAST_TWO_ID, 2)
        );
        ReorderPlaylistRequest request = new ReorderPlaylistRequest(List.of(
                new ReorderPlaylistRequest.Item(PODCAST_TWO_ID, 1),
                new ReorderPlaylistRequest.Item(PODCAST_ONE_ID, 2)
        ));
        stubOwnerMutation(playlist);
        stubDetailAfterMutation(playlist);
        when(playlistPodcastRepository.findByPlaylistIdForUpdate(PLAYLIST_ID)).thenReturn(existingItems);

        service.reorder(PLAYLIST_ID, USER_ID, request);

        InOrder inOrder = inOrder(playlistPodcastRepository);
        inOrder.verify(playlistPodcastRepository).findByPlaylistIdForUpdate(PLAYLIST_ID);
        inOrder.verify(playlistPodcastRepository).offsetPositions(PLAYLIST_ID, 3);
        inOrder.verify(playlistPodcastRepository).flush();
        inOrder.verify(playlistPodcastRepository).setPosition(PLAYLIST_ID, PODCAST_TWO_ID, 1);
        inOrder.verify(playlistPodcastRepository).setPosition(PLAYLIST_ID, PODCAST_ONE_ID, 2);
        inOrder.verify(playlistPodcastRepository).flush();
    }

    @Test
    void getPlaylistLoadsOnlyPublishedPodcastItems() {
        PlaylistEntity playlist = playlist();
        List<PlaylistPodcastEntity> visibleItems = List.of(playlistItem(PODCAST_ONE_ID, 1));
        when(playlistRepository.findWithOwnerById(PLAYLIST_ID)).thenReturn(Optional.of(playlist));
        when(playlistPodcastRepository.findVisibleByIdPlaylistIdOrderByPositionAsc(PLAYLIST_ID, Status.PUBLISHED))
                .thenReturn(visibleItems);
        when(playlistMapper.toDetail(eq(playlist), eq(visibleItems), eq(null), eq(null)))
                .thenReturn(new PlaylistDetailResponse(
                        PLAYLIST_ID,
                        playlist.getTitle(),
                        playlist.getCoverImageUrl(),
                        new PlaylistOwnerResponse(USER_PROFILE_ID, "dev-user"),
                        playlist.isPublicPlaylist(),
                        1L,
                        playlist.getLikesCount(),
                        playlist.getDislikesCount(),
                        playlist.getCreatedAt(),
                        null,
                        playlist.getDescription(),
                        playlist.getUpdatedAt(),
                        List.of()
                ));

        PlaylistDetailResponse response = service.get(PLAYLIST_ID, null);

        assertThat(response.id()).isEqualTo(PLAYLIST_ID);
        verify(playlistPodcastRepository).findVisibleByIdPlaylistIdOrderByPositionAsc(PLAYLIST_ID, Status.PUBLISHED);
    }

    private void stubOwnerMutation(PlaylistEntity playlist) {
        when(playlistRepository.findWithOwnerByIdForUpdate(PLAYLIST_ID)).thenReturn(Optional.of(playlist));
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(userProfile()));
    }

    private void stubDetailAfterMutation(PlaylistEntity playlist) {
        when(playlistRepository.findWithOwnerById(PLAYLIST_ID)).thenReturn(Optional.of(playlist));
        when(playlistPodcastRepository.findVisibleByIdPlaylistIdOrderByPositionAsc(PLAYLIST_ID, Status.PUBLISHED))
                .thenReturn(List.of());
        when(playlistMapper.toDetail(eq(playlist), any(), eq(USER_PROFILE_ID), any()))
                .thenReturn(new PlaylistDetailResponse(
                        PLAYLIST_ID,
                        playlist.getTitle(),
                        playlist.getCoverImageUrl(),
                        new PlaylistOwnerResponse(USER_PROFILE_ID, "dev-user"),
                        playlist.isPublicPlaylist(),
                        0L,
                        playlist.getLikesCount(),
                        playlist.getDislikesCount(),
                        playlist.getCreatedAt(),
                        null,
                        playlist.getDescription(),
                        playlist.getUpdatedAt(),
                        List.of()
                ));
    }

    private PlaylistEntity playlist() {
        PlaylistEntity entity = new PlaylistEntity();
        entity.setId(PLAYLIST_ID);
        entity.setOwner(userProfile());
        entity.setTitle("Original title");
        entity.setDescription("Original description");
        entity.setPublicPlaylist(true);
        return entity;
    }

    private UserProfileEntity userProfile() {
        UserProfileEntity entity = new UserProfileEntity();
        entity.setId(USER_PROFILE_ID);
        entity.setUserId(USER_ID);
        entity.setUsername("dev-user");
        return entity;
    }

    private PodcastEntity podcast(UUID id, Status status) {
        PodcastEntity entity = new PodcastEntity();
        entity.setId(id);
        entity.setTitle("Podcast");
        entity.setStatus(status);
        return entity;
    }

    private PlaylistPodcastEntity playlistItem(UUID podcastId, int position) {
        PlaylistPodcastEntity entity = new PlaylistPodcastEntity();
        entity.setId(new PlaylistPodcastId(PLAYLIST_ID, podcastId));
        entity.setPosition(position);
        return entity;
    }

    private PlaylistService serviceWithRecommendationEvents(boolean enabled) {
        OutboxEventService outboxEventService = new OutboxEventService(
                outboxEventRepository,
                new JacksonConfig().objectMapper()
        );
        RecommendationOutboxEventService recommendationOutboxEventService = new RecommendationOutboxEventService(
                outboxEventService,
                new RecommendationEventsProperties(enabled)
        );
        return new PlaylistService(
                playlistRepository,
                playlistPodcastRepository,
                playlistVoteRepository,
                savedPlaylistRepository,
                podcastRepository,
                userProfileRepository,
                authorRepository,
                playlistMapper,
                entityManager,
                recommendationOutboxEventService
        );
    }
}
