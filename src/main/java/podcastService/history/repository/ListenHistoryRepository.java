package podcastService.history.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import podcastService.history.entity.ListenHistoryEntity;
import podcastService.history.entity.ListenHistoryId;
import podcastService.podcast.entity.Status;

import java.util.Optional;
import java.util.UUID;

public interface ListenHistoryRepository extends JpaRepository<ListenHistoryEntity, ListenHistoryId> {

    Optional<ListenHistoryEntity> findByIdUserProfileIdAndIdPodcastId(UUID userProfileId, UUID podcastId);

    @Modifying
    @Query(
            value = """
                    insert into listen_history (user_profile_id, podcast_id, progress_seconds, completed)
                    values (:userProfileId, :podcastId, :progressSeconds, :completed)
                    on conflict (user_profile_id, podcast_id) do update
                       set progress_seconds = excluded.progress_seconds,
                           completed = excluded.completed
                    """,
            nativeQuery = true
    )
    void upsertProgress(
            @Param("userProfileId") UUID userProfileId,
            @Param("podcastId") UUID podcastId,
            @Param("progressSeconds") int progressSeconds,
            @Param("completed") boolean completed
    );

    @EntityGraph(attributePaths = {
            "podcast",
            "podcast.author",
            "podcast.author.userProfile",
            "podcast.category"
    })
    @Query(
            value = """
                    select h
                    from ListenHistoryEntity h
                    where h.id.userProfileId = :userProfileId
                      and h.podcast.status = :status
                    order by h.lastListenedAt desc
                    """,
            countQuery = """
                    select count(h)
                    from ListenHistoryEntity h
                    where h.id.userProfileId = :userProfileId
                      and h.podcast.status = :status
                    """
    )
    Page<ListenHistoryEntity> findVisibleByUserProfileId(
            @Param("userProfileId") UUID userProfileId,
            @Param("status") Status status,
            Pageable pageable
    );
}
