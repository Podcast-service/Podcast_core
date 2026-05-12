package podcastService.history.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import podcastService.podcast.entity.PodcastEntity;
import podcastService.user.entity.UserProfileEntity;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "listen_history")
public class ListenHistoryEntity {

    @EmbeddedId
    private ListenHistoryId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userProfileId")
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfileEntity userProfile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("podcastId")
    @JoinColumn(name = "podcast_id", nullable = false)
    private PodcastEntity podcast;

    @Column(name = "progress_seconds", nullable = false)
    private int progressSeconds;

    @Column(name = "completed", nullable = false)
    private boolean completed;

    @Column(name = "last_listened_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime lastListenedAt;
}
