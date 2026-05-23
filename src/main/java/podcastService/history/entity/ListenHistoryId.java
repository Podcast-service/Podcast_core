package podcastService.history.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
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
@AllArgsConstructor
@EqualsAndHashCode
public class ListenHistoryId implements Serializable {

    @Column(name = "user_profile_id", nullable = false)
    private UUID userProfileId;

    @Column(name = "podcast_id", nullable = false)
    private UUID podcastId;
}
