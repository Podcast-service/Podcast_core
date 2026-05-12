package podcastService.author.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import podcastService.author.entity.AuthorEntity;

import java.util.Optional;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AuthorRepository extends JpaRepository<AuthorEntity, UUID> {

    @EntityGraph(attributePaths = "userProfile")
    @Query("select a from AuthorEntity a where a.id = :id")
    Optional<AuthorEntity> findDetailedById(@Param("id") UUID id);

    @EntityGraph(attributePaths = "userProfile")
    @Query("select a from AuthorEntity a where a.id in :ids")
    List<AuthorEntity> findDetailedByIdIn(@Param("ids") Collection<UUID> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "userProfile")
    @Query("select a from AuthorEntity a where a.id = :id")
    Optional<AuthorEntity> findDetailedByIdForUpdate(@Param("id") UUID id);

    @Query("select a from AuthorEntity a where a.userProfile.id = :userProfileId")
    Optional<AuthorEntity> findByUserProfileId(@Param("userProfileId") UUID userProfileId);

    @Query("select count(a) > 0 from AuthorEntity a where a.userProfile.id = :userProfileId")
    boolean existsByUserProfileId(@Param("userProfileId") UUID userProfileId);

    @EntityGraph(attributePaths = "userProfile")
    @Query("select a from AuthorEntity a where a.userProfile.userId = :userId")
    Optional<AuthorEntity> findByUserProfileUserId(@Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "userProfile")
    @Query("select a from AuthorEntity a where a.userProfile.userId = :userId")
    Optional<AuthorEntity> findByUserProfileUserIdForUpdate(@Param("userId") UUID userId);
}
