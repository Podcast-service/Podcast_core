package podcastService.subscription.entity;

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
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class SubscriptionId implements Serializable {

    @Column(name = "subscriber_profile_id", nullable = false)
    private UUID subscriberProfileId;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;
}
