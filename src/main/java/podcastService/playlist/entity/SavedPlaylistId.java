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
public class SavedPlaylistId implements Serializable {

    @Column(name = "user_profile_id", nullable = false)
    private UUID userProfileId;

    @Column(name = "playlist_id", nullable = false)
    private UUID playlistId;

    public SavedPlaylistId(UUID userProfileId, UUID playlistId) {
        this.userProfileId = userProfileId;
        this.playlistId = playlistId;
    }
}
