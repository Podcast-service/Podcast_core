package podcastService.common.exception;

public class NotFoundException extends BaseException {
    public NotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message);
    }
}
