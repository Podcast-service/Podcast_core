package podcastService.transcript.entity;

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
public class PodcastSummaryId implements Serializable {

    @Column(name = "podcast_id", nullable = false)
    private UUID podcastId;

    @Column(name = "language", nullable = false, length = 5)
    private String language;
}
