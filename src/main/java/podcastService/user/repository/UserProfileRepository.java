package podcastService.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import podcastService.user.entity.UserProfileEntity;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfileEntity, UUID> {

    Optional<UserProfileEntity> findByUserId(UUID userId);

    @Modifying
    @Query(
            value = """
                    insert into user_profiles (user_id, username)
                    values (:userId, :username)
                    """,
            nativeQuery = true
    )
    void insertUserProfile(@Param("userId") UUID userId, @Param("username") String username);
}
