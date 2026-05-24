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
                    on conflict (user_id) do update
                       set username = excluded.username
                     where user_profiles.username is distinct from excluded.username
                    """,
            nativeQuery = true
    )
    void upsertUserProfile(@Param("userId") UUID userId, @Param("username") String username);

    @Modifying
    @Query(
            value = """
                    update user_profiles
                       set avatar_url = :avatarUrl
                     where id = :objectId
                        or user_id = :objectId
                    """,
            nativeQuery = true
    )
    int updateAvatarByProfileIdOrUserId(
            @Param("objectId") UUID objectId,
            @Param("avatarUrl") String avatarUrl
    );
}
