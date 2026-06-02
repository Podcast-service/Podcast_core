package podcastService.transcript.summary;

import podcastService.common.exception.UpstreamServiceException;

public class OpenRouterClientException extends UpstreamServiceException {

    public OpenRouterClientException(String message) {
        super(message);
    }

    public OpenRouterClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
