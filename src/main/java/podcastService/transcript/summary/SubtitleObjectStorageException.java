package podcastService.transcript.summary;

import podcastService.common.exception.UpstreamServiceException;

public class SubtitleObjectStorageException extends UpstreamServiceException {

    public SubtitleObjectStorageException(String message) {
        super(message);
    }

    public SubtitleObjectStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
