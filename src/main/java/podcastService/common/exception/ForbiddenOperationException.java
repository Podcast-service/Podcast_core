package podcastService.common.exception;

import java.util.Map;

public class ForbiddenOperationException extends BaseException {
    public ForbiddenOperationException(String message) {
        super(ErrorCode.FORBIDDEN, message);
    }

    public ForbiddenOperationException(String message, Map<String, Object> details) {
        super(ErrorCode.FORBIDDEN, message, details);
    }
}
