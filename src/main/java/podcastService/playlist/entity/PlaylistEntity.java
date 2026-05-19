package podcastService.playlist.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Formula;
import podcastService.user.entity.UserProfileEntity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "playlists")
public class PlaylistEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_profile_id", nullable = false)
    private UserProfileEntity owner;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "cover_image_url", columnDefinition = "text")
    private String coverImageUrl;

    @Column(name = "is_public", nullable = false)
    private boolean publicPlaylist;

    @Column(name = "likes_count", nullable = false)
    private long likesCount;

    @Column(name = "dislikes_count", nullable = false)
    private long dislikesCount;

    @Formula("(select count(*) from playlist_podcasts pp where pp.playlist_id = id)")
    private long podcastsCount;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "playlist", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlaylistPodcastEntity> podcasts = new ArrayList<>();
}
