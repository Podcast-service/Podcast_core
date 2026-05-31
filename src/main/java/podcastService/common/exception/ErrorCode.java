package podcastService.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND),
    AUTHOR_PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND),
    PLAYLIST_NOT_FOUND(HttpStatus.NOT_FOUND),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    CONFLICT(HttpStatus.CONFLICT),
    ALREADY_SAVED(HttpStatus.CONFLICT),
    FORBIDDEN(HttpStatus.FORBIDDEN),
    CANNOT_SAVE_OWN_PLAYLIST(HttpStatus.FORBIDDEN),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED),
    BUSINESS_RULE_VIOLATION(HttpStatus.UNPROCESSABLE_CONTENT),
    UPSTREAM_SERVICE_ERROR(HttpStatus.BAD_GATEWAY),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus httpStatus;

    ErrorCode(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }
}
