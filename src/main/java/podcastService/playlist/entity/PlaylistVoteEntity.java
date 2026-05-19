package podcastService.playlist.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import podcastService.user.entity.UserProfileEntity;
import podcastService.vote.dto.VoteType;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "playlist_votes")
public class PlaylistVoteEntity {

    @EmbeddedId
    private PlaylistVoteId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userProfileId")
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfileEntity userProfile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("playlistId")
    @JoinColumn(name = "playlist_id", nullable = false)
    private PlaylistEntity playlist;

    @Enumerated(EnumType.STRING)
    @Column(name = "vote_type", nullable = false, length = 10)
    private VoteType voteType;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}
