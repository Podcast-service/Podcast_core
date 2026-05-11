package podcastService.playlist.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import podcastService.playlist.entity.PlaylistVoteEntity;
import podcastService.playlist.entity.PlaylistVoteId;

import java.util.Optional;
import java.util.UUID;

public interface PlaylistVoteRepository extends JpaRepository<PlaylistVoteEntity, PlaylistVoteId> {

    Optional<PlaylistVoteEntity> findByIdUserProfileIdAndIdPlaylistId(UUID userProfileId, UUID playlistId);
}
