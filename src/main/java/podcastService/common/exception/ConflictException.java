package podcastService.common.exception;

import java.util.Map;

public class ConflictException extends BaseException {
    public ConflictException(String message) {
        super(ErrorCode.CONFLICT, message);
    }

    public ConflictException(String message, Map<String, Object> details) {
        super(ErrorCode.CONFLICT, message, details);
    }
}
