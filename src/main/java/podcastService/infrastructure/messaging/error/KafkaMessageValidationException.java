package podcastService.infrastructure.messaging.error;

public class KafkaMessageValidationException extends KafkaProcessingException {
    public KafkaMessageValidationException(String message) {
        super(message);
    }
}
