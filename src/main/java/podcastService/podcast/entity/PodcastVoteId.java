package podcastService.podcast.entity;

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
public class PodcastVoteId implements Serializable {

    @Column(name = "user_profile_id", nullable = false)
    private UUID userProfileId;

    @Column(name = "podcast_id", nullable = false)
    private UUID podcastId;

    public PodcastVoteId(UUID userProfileId, UUID podcastId) {
        this.userProfileId = userProfileId;
        this.podcastId = podcastId;
    }
}
