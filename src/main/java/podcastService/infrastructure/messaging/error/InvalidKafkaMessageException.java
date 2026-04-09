package podcastService.infrastructure.messaging.error;

public class InvalidKafkaMessageException extends KafkaProcessingException {
    public InvalidKafkaMessageException(String message) {
        super(message);
    }

    public InvalidKafkaMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
