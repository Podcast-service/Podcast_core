package podcastService.podcast.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import podcastService.podcast.entity.PodcastEntity;
import podcastService.podcast.entity.PodcastVoteEntity;
import podcastService.podcast.entity.PodcastVoteId;
import podcastService.podcast.entity.Status;
import podcastService.vote.dto.VoteType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PodcastVoteRepository extends JpaRepository<PodcastVoteEntity, PodcastVoteId> {

    Optional<PodcastVoteEntity> findByIdUserProfileIdAndIdPodcastId(UUID userProfileId, UUID podcastId);

    @Query("""
            select v
            from PodcastVoteEntity v
            where v.id.userProfileId = :userProfileId
              and v.id.podcastId in :podcastIds
            """)
    List<PodcastVoteEntity> findByUserProfileIdAndPodcastIds(
            @Param("userProfileId") UUID userProfileId,
            @Param("podcastIds") Collection<UUID> podcastIds
    );

    @Query(
            value = """
            select p
            from PodcastVoteEntity v
            join v.podcast p
            join fetch p.author a
            join fetch a.userProfile
            left join fetch p.category
            where v.id.userProfileId = :userProfileId
              and v.voteType = :voteType
              and p.status = :status
            """,
            countQuery = """
            select count(p)
            from PodcastVoteEntity v
            join v.podcast p
            where v.id.userProfileId = :userProfileId
              and v.voteType = :voteType
              and p.status = :status
            """
    )
    Page<PodcastEntity> findVotedPodcasts(
            @Param("userProfileId") UUID userProfileId,
            @Param("voteType") VoteType voteType,
            @Param("status") Status status,
            Pageable pageable
    );

    @Query(
            value = """
            select p
            from PodcastVoteEntity v
            join v.podcast p
            join fetch p.author a
            join fetch a.userProfile
            left join fetch p.category
            where v.id.userProfileId = :userProfileId
              and v.voteType = :voteType
              and p.status = :status
            order by v.createdAt desc, p.id desc
            """,
            countQuery = """
            select count(p)
            from PodcastVoteEntity v
            join v.podcast p
            where v.id.userProfileId = :userProfileId
              and v.voteType = :voteType
              and p.status = :status
            """
    )
    Page<PodcastEntity> findVotedPodcastsByVoteDateDesc(
            @Param("userProfileId") UUID userProfileId,
            @Param("voteType") VoteType voteType,
            @Param("status") Status status,
            Pageable pageable
    );

    @Query(
            value = """
            select p
            from PodcastVoteEntity v
            join v.podcast p
            join fetch p.author a
            join fetch a.userProfile
            left join fetch p.category
            where v.id.userProfileId = :userProfileId
              and v.voteType = :voteType
              and p.status = :status
            order by v.createdAt asc, p.id asc
            """,
            countQuery = """
            select count(p)
            from PodcastVoteEntity v
            join v.podcast p
            where v.id.userProfileId = :userProfileId
              and v.voteType = :voteType
              and p.status = :status
            """
    )
    Page<PodcastEntity> findVotedPodcastsByVoteDateAsc(
            @Param("userProfileId") UUID userProfileId,
            @Param("voteType") VoteType voteType,
            @Param("status") Status status,
            Pageable pageable
    );
}
