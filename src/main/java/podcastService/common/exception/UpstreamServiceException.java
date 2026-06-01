package podcastService.common.exception;

public class UpstreamServiceException extends BaseException {
    public UpstreamServiceException(String message) {
        super(ErrorCode.UPSTREAM_SERVICE_ERROR, message);
    }

    public UpstreamServiceException(String message, Throwable cause) {
        super(ErrorCode.UPSTREAM_SERVICE_ERROR, message);
        initCause(cause);
    }
}
