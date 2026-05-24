package podcastService.media.messaging.handler;

import org.springframework.stereotype.Component;
import podcastService.media.messaging.contract.MediaObjectType;
import podcastService.media.messaging.contract.MediaUploadEventType;

@Component
public class PodcastCoverUploadErrorHandler extends NoopMediaUploadEventHandler {
    public PodcastCoverUploadErrorHandler() {
        super(MediaObjectType.PODCAST_COVER_URL, MediaUploadEventType.ERROR);
    }
}
