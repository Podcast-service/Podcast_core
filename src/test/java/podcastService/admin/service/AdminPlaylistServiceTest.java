package podcastService.admin.service;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import podcastService.admin.dto.AdminPageResponse;
import podcastService.admin.dto.AdminPlaylistFilter;
import podcastService.admin.dto.AdminPlaylistResponse;
import podcastService.admin.mapper.AdminMapper;
import podcastService.infrastructure.outbox.recommendation.RecommendationOutboxEventService;
import podcastService.playlist.dto.SortPlaylists;
import podcastService.playlist.entity.PlaylistEntity;
import podcastService.playlist.repository.PlaylistPodcastRepository;
import podcastService.playlist.repository.PlaylistRepository;
import podcastService.user.entity.UserProfileEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminPlaylistServiceTest {

    private static final UUID ADMIN_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PLAYLIST_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID OWNER_PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");

    private final PlaylistRepository playlistRepository = mock(PlaylistRepository.class);
    private final PlaylistPodcastRepository playlistPodcastRepository = mock(PlaylistPodcastRepository.class);
    private final RecommendationOutboxEventService recommendationOutboxEventService =
            mock(RecommendationOutboxEventService.class);
    private final AdminPlaylistService service = new AdminPlaylistService(
            playlistRepository,
            playlistPodcastRepository,
            recommendationOutboxEventService,
            new AdminMapper()
    );

    @SuppressWarnings("unchecked")
    @Test
    void getPlaylistsReturnsPrivatePlaylistsForAdmin() {
        PlaylistEntity playlist = playlist(false);
        when(playlistRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(playlist)));

        AdminPageResponse<AdminPlaylistResponse> response = service.getPlaylists(new AdminPlaylistFilter(
                null,
                null,
                null,
                0,
                20,
                SortPlaylists.DATE_DESC
        ));

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().id()).isEqualTo(PLAYLIST_ID);
        assertThat(response.items().getFirst().isPublic()).isFalse();
        assertThat(response.items().getFirst().owner().profileId()).isEqualTo(OWNER_PROFILE_ID);
    }

    @Test
    void deletePlaylistDeletesAnyPlaylistWithoutOwnerCheck() {
        PlaylistEntity playlist = playlist(true);
        when(playlistRepository.findWithOwnerByIdForUpdate(PLAYLIST_ID)).thenReturn(Optional.of(playlist));

        service.deletePlaylist(PLAYLIST_ID, ADMIN_USER_ID);

        verify(playlistRepository).delete(playlist);
        verify(playlistRepository).flush();
        verify(recommendationOutboxEventService).savePlaylistEvent(any(), any());
    }

    private PlaylistEntity playlist(boolean isPublic) {
        UserProfileEntity owner = new UserProfileEntity();
        owner.setId(OWNER_PROFILE_ID);
        owner.setUserId(UUID.fromString("00000000-0000-0000-0000-000000000301"));
        owner.setUsername("playlist-owner");

        PlaylistEntity playlist = new PlaylistEntity();
        playlist.setId(PLAYLIST_ID);
        playlist.setOwner(owner);
        playlist.setTitle("Backend essentials");
        playlist.setDescription("Playlist description");
        playlist.setPublicPlaylist(isPublic);
        return playlist;
    }
}
