package podcastService.common.exception;

public class AlreadySavedException extends BaseException {
    public AlreadySavedException(String message) {
        super(ErrorCode.ALREADY_SAVED, message);
    }
}
