package podcastService.media.messaging.handler;

import org.springframework.stereotype.Component;
import podcastService.media.messaging.contract.MediaObjectType;
import podcastService.media.messaging.contract.MediaUploadEventType;

@Component
public class PodcastCoverStartUploadHandler extends NoopMediaUploadEventHandler {
    public PodcastCoverStartUploadHandler() {
        super(MediaObjectType.PODCAST_COVER_URL, MediaUploadEventType.START_UPLOAD);
    }
}
