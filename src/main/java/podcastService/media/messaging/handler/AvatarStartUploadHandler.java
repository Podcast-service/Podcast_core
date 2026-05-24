package podcastService.media.messaging.handler;

import org.springframework.stereotype.Component;

@Component
public class AvatarStartUploadHandler extends MediaNoopHandler {
    public AvatarStartUploadHandler() {
        super("avatar", "start_upload");
    }
}
