package podcastService.infrastructure.messaging.error;

public abstract class KafkaProcessingException extends RuntimeException {

    protected KafkaProcessingException(String message) {
        super(message);
    }

    protected KafkaProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
