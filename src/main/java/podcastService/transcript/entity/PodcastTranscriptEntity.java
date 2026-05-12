package podcastService.transcript.entity;

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
@Table(name = "podcast_transcripts")
public class PodcastTranscriptEntity {

    @EmbeddedId
    private PodcastTranscriptId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("podcastId")
    @JoinColumn(name = "podcast_id", nullable = false)
    private PodcastEntity podcast;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "generated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime generatedAt;
}
