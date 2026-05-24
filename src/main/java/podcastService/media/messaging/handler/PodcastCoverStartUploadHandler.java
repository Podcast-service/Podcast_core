package podcastService.media.messaging.handler;

import org.springframework.stereotype.Component;

@Component
public class PodcastCoverStartUploadHandler extends MediaNoopHandler {
    public PodcastCoverStartUploadHandler() {
        super("podcast_cover", "start_upload");
    }
}
