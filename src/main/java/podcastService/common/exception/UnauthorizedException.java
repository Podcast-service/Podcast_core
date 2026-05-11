package podcastService.common.exception;

import java.util.Map;

public class UnauthorizedException extends BaseException {
    public UnauthorizedException(String message) {
        super(ErrorCode.UNAUTHORIZED, message);
    }

    public UnauthorizedException(String message, Map<String, Object> details) {
        super(ErrorCode.UNAUTHORIZED, message, details);
    }
}
