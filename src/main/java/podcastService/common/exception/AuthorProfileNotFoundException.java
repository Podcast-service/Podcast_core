package podcastService.common.exception;

public class AuthorProfileNotFoundException extends BaseException {
    public AuthorProfileNotFoundException(String message) {
        super(ErrorCode.AUTHOR_PROFILE_NOT_FOUND, message);
    }
}
