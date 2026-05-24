package podcastService.media.messaging.handler;

import org.springframework.stereotype.Component;

@Component
public class PlaylistErrorHandler extends MediaNoopHandler {
    public PlaylistErrorHandler() {
        super("playlists", "error");
    }
}
