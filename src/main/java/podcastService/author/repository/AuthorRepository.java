package podcastService.author.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import podcastService.author.entity.AuthorEntity;

import java.util.Optional;
import java.util.UUID;

public interface AuthorRepository extends JpaRepository<AuthorEntity, UUID> {

    Optional<AuthorEntity> findByUserProfileId(UUID userProfileId);

    Optional<AuthorEntity> findByUserProfileUserId(UUID userId);
}
