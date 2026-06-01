package podcastService.playlist.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import podcastService.author.repository.AuthorRepository;
import podcastService.common.exception.AlreadySavedException;
import podcastService.common.exception.CannotSaveOwnPlaylistException;
import podcastService.infrastructure.outbox.recommendation.RecommendationOutboxEventService;
import podcastService.playlist.dto.PlaylistSaveResponse;
import podcastService.playlist.entity.PlaylistEntity;
import podcastService.playlist.entity.SavedPlaylistEntity;
import podcastService.playlist.mapper.PlaylistMapper;
import podcastService.playlist.repository.PlaylistPodcastRepository;
import podcastService.playlist.repository.PlaylistRepository;
import podcastService.playlist.repository.PlaylistVoteRepository;
import podcastService.playlist.repository.SavedPlaylistRepository;
import podcastService.podcast.repository.PodcastRepository;
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
class PlaylistServiceSavedLibraryTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID USER_PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID OWNER_PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000102");
    private static final UUID PLAYLIST_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");

    @Mock private PlaylistRepository playlistRepository;
    @Mock private PlaylistPodcastRepository playlistPodcastRepository;
    @Mock private PlaylistVoteRepository playlistVoteRepository;
    @Mock private SavedPlaylistRepository savedPlaylistRepository;
    @Mock private PodcastRepository podcastRepository;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private AuthorRepository authorRepository;
    @Mock private PlaylistMapper playlistMapper;
    @Mock private EntityManager entityManager;
    @Mock private RecommendationOutboxEventService recommendationOutboxEventService;

    private PlaylistService service;

    @BeforeEach
    void setUp() {
        service = new PlaylistService(
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

    @Test
    void saveToLibraryStoresForeignPublicPlaylist() {
        UserProfileEntity currentUser = userProfile(USER_PROFILE_ID);
        PlaylistEntity playlist = playlist(OWNER_PROFILE_ID, true);

        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(currentUser));
        when(playlistRepository.findWithOwnerById(PLAYLIST_ID)).thenReturn(Optional.of(playlist));
        when(savedPlaylistRepository.existsByIdUserProfileIdAndIdPlaylistId(USER_PROFILE_ID, PLAYLIST_ID))
                .thenReturn(false);
        when(savedPlaylistRepository.saveAndFlush(any(SavedPlaylistEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PlaylistSaveResponse response = service.saveToLibrary(PLAYLIST_ID, USER_ID);

        assertThat(response.playlistId()).isEqualTo(PLAYLIST_ID);
        assertThat(response.isSaved()).isTrue();

        ArgumentCaptor<SavedPlaylistEntity> captor = ArgumentCaptor.forClass(SavedPlaylistEntity.class);
        verify(savedPlaylistRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getId().getUserProfileId()).isEqualTo(USER_PROFILE_ID);
        assertThat(captor.getValue().getId().getPlaylistId()).isEqualTo(PLAYLIST_ID);
    }

    @Test
    void saveToLibraryRejectsOwnPlaylist() {
        UserProfileEntity currentUser = userProfile(USER_PROFILE_ID);
        PlaylistEntity playlist = playlist(USER_PROFILE_ID, true);

        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(currentUser));
        when(playlistRepository.findWithOwnerById(PLAYLIST_ID)).thenReturn(Optional.of(playlist));

        assertThatThrownBy(() -> service.saveToLibrary(PLAYLIST_ID, USER_ID))
                .isInstanceOf(CannotSaveOwnPlaylistException.class);
    }

    @Test
    void saveToLibraryRejectsAlreadySavedPlaylist() {
        UserProfileEntity currentUser = userProfile(USER_PROFILE_ID);
        PlaylistEntity playlist = playlist(OWNER_PROFILE_ID, true);

        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(currentUser));
        when(playlistRepository.findWithOwnerById(PLAYLIST_ID)).thenReturn(Optional.of(playlist));
        when(savedPlaylistRepository.existsByIdUserProfileIdAndIdPlaylistId(USER_PROFILE_ID, PLAYLIST_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> service.saveToLibrary(PLAYLIST_ID, USER_ID))
                .isInstanceOf(AlreadySavedException.class);
    }

    @Test
    void removeFromLibraryIsIdempotentForExistingVisiblePlaylist() {
        UserProfileEntity currentUser = userProfile(USER_PROFILE_ID);
        PlaylistEntity playlist = playlist(OWNER_PROFILE_ID, true);

        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(currentUser));
        when(playlistRepository.findWithOwnerById(PLAYLIST_ID)).thenReturn(Optional.of(playlist));

        PlaylistSaveResponse response = service.removeFromLibrary(PLAYLIST_ID, USER_ID);

        assertThat(response.playlistId()).isEqualTo(PLAYLIST_ID);
        assertThat(response.isSaved()).isFalse();
        verify(savedPlaylistRepository).deleteByIdUserProfileIdAndIdPlaylistId(USER_PROFILE_ID, PLAYLIST_ID);
        verify(savedPlaylistRepository).flush();
    }

    private UserProfileEntity userProfile(UUID id) {
        UserProfileEntity entity = new UserProfileEntity();
        entity.setId(id);
        entity.setUserId(USER_ID);
        entity.setUsername("dev-user");
        return entity;
    }

    private PlaylistEntity playlist(UUID ownerProfileId, boolean publicPlaylist) {
        PlaylistEntity entity = new PlaylistEntity();
        entity.setId(PLAYLIST_ID);
        entity.setOwner(userProfile(ownerProfileId));
        entity.setPublicPlaylist(publicPlaylist);
        entity.setTitle("Public playlist");
        return entity;
    }
}
