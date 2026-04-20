package podcastService.author.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import podcastService.user.entity.UserProfileEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "author_profiles")
public class AuthorEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_profile_id", nullable = false, unique = true)
    private UserProfileEntity userProfile;

    @Column(name = "author_name", nullable = false, length = 100)
    private String authorName;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "subscribers_count", nullable = false)
    private long subscribersCount;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;
}
