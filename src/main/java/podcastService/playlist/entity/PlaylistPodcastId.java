package podcastService.playlist.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@Embeddable
@NoArgsConstructor
@EqualsAndHashCode
public class PlaylistPodcastId implements Serializable {

    @Column(name = "playlist_id", nullable = false)
    private UUID playlistId;

    @Column(name = "podcast_id", nullable = false)
    private UUID podcastId;

    public PlaylistPodcastId(UUID playlistId, UUID podcastId) {
        this.playlistId = playlistId;
        this.podcastId = podcastId;
    }
}
