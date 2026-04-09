package podcastService.infrastructure.messaging.error;

public class KafkaPublishException extends KafkaProcessingException {
    public KafkaPublishException(String message, Throwable cause) {
        super(message, cause);
    }
}
