package podcastService.media.messaging.handler;

import org.springframework.stereotype.Component;
import podcastService.media.messaging.contract.MediaObjectType;
import podcastService.media.messaging.contract.MediaUploadEventType;

@Component
public class PlaylistUploadFailedHandler extends NoopMediaUploadEventHandler {
    public PlaylistUploadFailedHandler() {
        super(MediaObjectType.PLAYLIST, MediaUploadEventType.UPLOAD_FAILED);
    }
}
