package podcastService.media.messaging.handler;

import org.springframework.stereotype.Component;
import podcastService.media.messaging.contract.MediaObjectType;
import podcastService.media.messaging.contract.MediaWorkerEventType;

@Component
public class PodcastConvertedIgnoredHandler extends NoopMediaWorkerEventHandler {

    public PodcastConvertedIgnoredHandler() {
        super(MediaObjectType.PODCAST_FILE_URL, MediaWorkerEventType.CONVERTED);
    }
}
