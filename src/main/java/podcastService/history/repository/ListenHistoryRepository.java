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
                    insert into listen_history (user_profile_id, podcast_id, progress_seconds, completed, view_counted)
                    values (:userProfileId, :podcastId, :progressSeconds, :completed, false)
                    on conflict (user_profile_id, podcast_id) do update
                       set progress_seconds = excluded.progress_seconds,
                           completed = excluded.completed,
                           view_counted = listen_history.view_counted
                    """,
            nativeQuery = true
    )
    void upsertProgress(
            @Param("userProfileId") UUID userProfileId,
            @Param("podcastId") UUID podcastId,
            @Param("progressSeconds") int progressSeconds,
            @Param("completed") boolean completed
    );

    @Modifying
    @Query(
            value = """
                    update listen_history
                       set view_counted = true
                     where user_profile_id = :userProfileId
                       and podcast_id = :podcastId
                       and view_counted = false
                       and progress_seconds > 0
                    """,
            nativeQuery = true
    )
    int markViewCountedIfFirstPositiveProgress(
            @Param("userProfileId") UUID userProfileId,
            @Param("podcastId") UUID podcastId
    );

    @Modifying
    @Query(
            value = """
                    update podcasts
                       set views_count = coalesce(views_count, 0) + 1
                     where id = :podcastId
                    """,
            nativeQuery = true
    )
    int incrementPodcastViewsCount(@Param("podcastId") UUID podcastId);

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
