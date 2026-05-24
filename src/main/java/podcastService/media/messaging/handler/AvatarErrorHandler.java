package podcastService.media.messaging.handler;

import org.springframework.stereotype.Component;

@Component
public class AvatarErrorHandler extends MediaNoopHandler {
    public AvatarErrorHandler() {
        super("avatar", "error");
    }
}
