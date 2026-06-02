package podcastService.playlist.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import podcastService.playlist.entity.PlaylistPodcastEntity;
import podcastService.playlist.entity.PlaylistPodcastId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlaylistPodcastRepository extends JpaRepository<PlaylistPodcastEntity, PlaylistPodcastId> {

    boolean existsByIdPlaylistIdAndIdPodcastId(UUID playlistId, UUID podcastId);

    @EntityGraph(attributePaths = {
            "podcast",
            "podcast.author",
            "podcast.author.userProfile",
            "podcast.category"
    })
    List<PlaylistPodcastEntity> findByIdPlaylistIdOrderByPositionAsc(UUID playlistId);

    @Query(value = """
            select *
              from playlist_podcasts
             where playlist_id = :playlistId
             order by position asc
             for update
            """, nativeQuery = true)
    List<PlaylistPodcastEntity> findByPlaylistIdForUpdate(UUID playlistId);

    @Query("select coalesce(max(pp.position), 0) from PlaylistPodcastEntity pp where pp.id.playlistId = :playlistId")
    int findMaxPosition(UUID playlistId);

    @Query("""
            select pp.id.podcastId
              from PlaylistPodcastEntity pp
             where pp.id.playlistId = :playlistId
             order by pp.position asc
            """)
    List<UUID> findPodcastIdsByPlaylistIdOrderByPositionAsc(@Param("playlistId") UUID playlistId);

    Optional<PlaylistPodcastEntity> findByIdPlaylistIdAndIdPodcastId(UUID playlistId, UUID podcastId);

    @Modifying
    @Query("""
            update PlaylistPodcastEntity pp
               set pp.position = pp.position - 1
             where pp.id.playlistId = :playlistId
               and pp.position > :removedPosition
            """)
    int closeGapAfterDelete(@Param("playlistId") UUID playlistId, @Param("removedPosition") int removedPosition);

    @Modifying
    @Query("""
            update PlaylistPodcastEntity pp
               set pp.position = pp.position + :offset
             where pp.id.playlistId = :playlistId
            """)
    int offsetPositions(@Param("playlistId") UUID playlistId, @Param("offset") int offset);

    @Modifying
    @Query("""
            update PlaylistPodcastEntity pp
               set pp.position = :position
             where pp.id.playlistId = :playlistId
               and pp.id.podcastId = :podcastId
            """)
    int setPosition(
            @Param("playlistId") UUID playlistId,
            @Param("podcastId") UUID podcastId,
            @Param("position") int position
    );
}
