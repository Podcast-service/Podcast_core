package podcastService.user.messaging;

import java.util.UUID;

public record UserRegisteredEvent(
        UUID userId,
        String username
) {
}
