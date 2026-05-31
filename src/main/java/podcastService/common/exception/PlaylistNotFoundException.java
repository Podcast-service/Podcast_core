package podcastService.common.exception;

public class PlaylistNotFoundException extends BaseException {
    public PlaylistNotFoundException(String message) {
        super(ErrorCode.PLAYLIST_NOT_FOUND, message);
    }
}
