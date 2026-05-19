package podcastService.infrastructure.messaging.error;

public class KafkaDeserializationException extends KafkaProcessingException {

    public KafkaDeserializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
