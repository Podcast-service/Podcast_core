package podcastService.playlist.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import podcastService.playlist.entity.SavedPlaylistEntity;
import podcastService.playlist.entity.SavedPlaylistId;

import java.util.UUID;

public interface SavedPlaylistRepository extends JpaRepository<SavedPlaylistEntity, SavedPlaylistId> {

    boolean existsByIdUserProfileIdAndIdPlaylistId(UUID userProfileId, UUID playlistId);

    void deleteByIdUserProfileIdAndIdPlaylistId(UUID userProfileId, UUID playlistId);

    @EntityGraph(attributePaths = {
            "playlist",
            "playlist.owner"
    })
    Page<SavedPlaylistEntity> findByIdUserProfileIdOrderBySavedAtDesc(UUID userProfileId, Pageable pageable);
}
