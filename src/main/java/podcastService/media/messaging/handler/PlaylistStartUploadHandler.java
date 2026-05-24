package podcastService.media.messaging.handler;

import org.springframework.stereotype.Component;

@Component
public class PlaylistStartUploadHandler extends MediaNoopHandler {
    public PlaylistStartUploadHandler() {
        super("playlists", "start_upload");
    }
}
