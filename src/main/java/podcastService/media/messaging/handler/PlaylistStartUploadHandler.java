package podcastService.media.messaging.handler;

import org.springframework.stereotype.Component;
import podcastService.media.messaging.contract.MediaObjectType;
import podcastService.media.messaging.contract.MediaUploadEventType;

@Component
public class PlaylistStartUploadHandler extends NoopMediaUploadEventHandler {
    public PlaylistStartUploadHandler() {
        super(MediaObjectType.PLAYLIST, MediaUploadEventType.START_UPLOAD);
    }
}
