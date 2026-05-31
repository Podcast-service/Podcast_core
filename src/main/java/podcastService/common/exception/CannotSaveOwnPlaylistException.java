package podcastService.common.exception;

public class CannotSaveOwnPlaylistException extends BaseException {
    public CannotSaveOwnPlaylistException(String message) {
        super(ErrorCode.CANNOT_SAVE_OWN_PLAYLIST, message);
    }
}
