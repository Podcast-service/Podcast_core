package podcastService.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    CONFLICT(HttpStatus.CONFLICT),
    FORBIDDEN(HttpStatus.FORBIDDEN),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED),
    BUSINESS_RULE_VIOLATION(HttpStatus.UNPROCESSABLE_CONTENT),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus httpStatus;

    ErrorCode(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }
}
