package podcastService.common.exception;

public class UpstreamServiceException extends BaseException {
    public UpstreamServiceException(String message) {
        super(ErrorCode.UPSTREAM_SERVICE_ERROR, message);
    }
}
