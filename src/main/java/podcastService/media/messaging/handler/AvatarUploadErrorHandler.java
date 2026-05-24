package podcastService.media.messaging.handler;

import org.springframework.stereotype.Component;
import podcastService.media.messaging.contract.MediaObjectType;
import podcastService.media.messaging.contract.MediaUploadEventType;

@Component
public class AvatarUploadErrorHandler extends NoopMediaUploadEventHandler {
    public AvatarUploadErrorHandler() {
        super(MediaObjectType.AVATAR, MediaUploadEventType.ERROR);
    }
}
