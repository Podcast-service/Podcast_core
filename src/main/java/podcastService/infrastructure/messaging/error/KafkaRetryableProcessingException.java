package podcastService.infrastructure.messaging.error;

public class KafkaRetryableProcessingException extends KafkaProcessingException {
    public KafkaRetryableProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
