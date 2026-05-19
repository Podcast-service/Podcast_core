package podcastService.subscription.entity;

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
import podcastService.author.entity.AuthorEntity;
import podcastService.user.entity.UserProfileEntity;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "subscriptions")
public class SubscriptionEntity {

    @EmbeddedId
    private SubscriptionId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("subscriberProfileId")
    @JoinColumn(name = "subscriber_profile_id", nullable = false)
    private UserProfileEntity subscriber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("authorId")
    @JoinColumn(name = "author_id", nullable = false)
    private AuthorEntity author;

    @Column(name = "subscribed_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime subscribedAt;
}
