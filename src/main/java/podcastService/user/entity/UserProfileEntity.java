package podcastService.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicInsert;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@DynamicInsert
@Table(name = "user_profiles")
public class UserProfileEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "avatar_url", columnDefinition = "text")
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "theme", length = 10, nullable = false, insertable = false)
    private Theme theme;

    @Enumerated(EnumType.STRING)
    @Column(name = "language", length = 5, nullable = false, insertable = false)
    private Language language;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false)
    private OffsetDateTime updatedAt;
}
