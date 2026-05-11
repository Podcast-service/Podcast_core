package podcastService.author.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import podcastService.author.entity.AuthorEntity;

import java.util.Optional;
import java.util.UUID;

public interface AuthorRepository extends JpaRepository<AuthorEntity, UUID> {

    @EntityGraph(attributePaths = "userProfile")
    @Query("select a from AuthorEntity a where a.id = :id")
    Optional<AuthorEntity> findDetailedById(@Param("id") UUID id);

    Optional<AuthorEntity> findByUserProfileId(UUID userProfileId);

    boolean existsByUserProfileId(UUID userProfileId);

    @EntityGraph(attributePaths = "userProfile")
    Optional<AuthorEntity> findByUserProfileUserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "userProfile")
    @Query("select a from AuthorEntity a where a.userProfile.userId = :userId")
    Optional<AuthorEntity> findByUserProfileUserIdForUpdate(@Param("userId") UUID userId);
}
