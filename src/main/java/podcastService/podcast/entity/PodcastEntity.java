package podcastService.podcast.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import podcastService.author.entity.AuthorEntity;
import podcastService.category.entity.CategoryEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "podcasts")
public class PodcastEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private AuthorEntity author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private CategoryEntity category;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "cover_image_url", columnDefinition = "text")
    private String coverImageUrl;

    @Column(name = "audio_url", columnDefinition = "text")
    private String audioUrl;

    @Column(name = "audio_url_file", columnDefinition = "text")
    private String audioUrlFile;

    @Column(name = "audio_size_file")
    private Long audioSizeFile;

    @Column(name = "num_speakers", nullable = false)
    private Integer numSpeakers;

    @Column(name = "duration_seconds")
    private Long durationSeconds;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @Column(name = "views_count", nullable = false)
    private long viewsCount;

    @Column(name = "likes_count", nullable = false)
    private long likesCount;

    @Column(name = "dislikes_count", nullable = false)
    private long dislikesCount;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false)
    private OffsetDateTime updatedAt;
}
