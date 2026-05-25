package podcastService.media.messaging.handler;

import org.springframework.stereotype.Component;
import podcastService.media.messaging.contract.MediaObjectType;
import podcastService.media.messaging.contract.MediaUploadEventType;

@Component
public class AvatarUploadFailedHandler extends NoopMediaUploadEventHandler {
    public AvatarUploadFailedHandler() {
        super(MediaObjectType.AVATAR, MediaUploadEventType.UPLOAD_FAILED);
    }
}
