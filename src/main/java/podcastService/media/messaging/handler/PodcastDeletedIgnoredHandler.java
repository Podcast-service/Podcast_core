package podcastService.media.messaging.handler;

import org.springframework.stereotype.Component;
import podcastService.media.messaging.contract.MediaObjectType;
import podcastService.media.messaging.contract.MediaWorkerEventType;

@Component
public class PodcastDeletedIgnoredHandler extends NoopMediaWorkerEventHandler {

    public PodcastDeletedIgnoredHandler() {
        super(MediaObjectType.PODCAST_FILE_URL, MediaWorkerEventType.DELETED);
    }
}
