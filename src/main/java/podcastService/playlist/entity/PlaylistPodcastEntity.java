package podcastService.playlist.entity;

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

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "playlist_podcasts")
public class PlaylistPodcastEntity {

    @EmbeddedId
    private PlaylistPodcastId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("playlistId")
    @JoinColumn(name = "playlist_id", nullable = false)
    private PlaylistEntity playlist;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("podcastId")
    @JoinColumn(name = "podcast_id", nullable = false)
    private PodcastEntity podcast;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "added_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime addedAt;
}
