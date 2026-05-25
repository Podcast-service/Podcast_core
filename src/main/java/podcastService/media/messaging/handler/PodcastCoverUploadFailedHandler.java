package podcastService.media.messaging.handler;

import org.springframework.stereotype.Component;
import podcastService.media.messaging.contract.MediaObjectType;
import podcastService.media.messaging.contract.MediaUploadEventType;

@Component
public class PodcastCoverUploadFailedHandler extends NoopMediaUploadEventHandler {
    public PodcastCoverUploadFailedHandler() {
        super(MediaObjectType.PODCAST_COVER_URL, MediaUploadEventType.UPLOAD_FAILED);
    }
}
