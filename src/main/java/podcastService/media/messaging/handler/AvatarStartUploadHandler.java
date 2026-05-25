package podcastService.media.messaging.handler;

import org.springframework.stereotype.Component;
import podcastService.media.messaging.contract.MediaObjectType;
import podcastService.media.messaging.contract.MediaUploadEventType;

@Component
public class AvatarStartUploadHandler extends NoopMediaUploadEventHandler {
    public AvatarStartUploadHandler() {
        super(MediaObjectType.AVATAR, MediaUploadEventType.START_UPLOAD);
    }
}
