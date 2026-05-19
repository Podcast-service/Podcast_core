package podcastService.common.exception;

import java.util.Map;

public class BadRequestException extends BaseException {
    public BadRequestException(String message) {
        super(ErrorCode.VALIDATION_ERROR, message);
    }

    public BadRequestException(String message, Map<String, Object> details) {
        super(ErrorCode.VALIDATION_ERROR, message, details);
    }
}
