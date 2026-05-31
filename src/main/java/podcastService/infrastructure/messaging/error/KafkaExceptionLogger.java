package podcastService.infrastructure.messaging.error;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class KafkaExceptionLogger {

    private static final Logger log = LoggerFactory.getLogger(KafkaExceptionLogger.class);

    public void logRetryAttempt(ConsumerRecord<?, ?> record, Exception exception, int deliveryAttempt) {
        Throwable cause = rootCause(exception);
        log.warn(
                "Kafka processing failed, will retry. topic={}, partition={}, offset={}, key={}, deliveryAttempt={}, exception={}, message={}, rootCause={}, rootMessage={}",
                record.topic(),
                record.partition(),
                record.offset(),
                record.key(),
                deliveryAttempt,
                exception.getClass().getSimpleName(),
                exception.getMessage(),
                cause.getClass().getSimpleName(),
                cause.getMessage()
        );
    }

    public void logSentToDlt(ConsumerRecord<?, ?> record, Exception exception, String dltTopic) {
        Throwable cause = rootCause(exception);
        log.error(
                "Kafka message redirected to DLT. sourceTopic={}, dltTopic={}, partition={}, offset={}, key={}, exception={}, message={}, rootCause={}, rootMessage={}",
                record.topic(),
                dltTopic,
                record.partition(),
                record.offset(),
                record.key(),
                exception.getClass().getSimpleName(),
                exception.getMessage(),
                cause.getClass().getSimpleName(),
                cause.getMessage(),
                exception
        );
    }

    public void logSkippedInvalidMessage(ConsumerRecord<?, ?> record, Exception exception) {
        Throwable cause = rootCause(exception);
        log.warn(
                "Kafka message skipped because it does not match expected contract. topic={}, partition={}, offset={}, key={}, exception={}, message={}, rootCause={}, rootMessage={}",
                record.topic(),
                record.partition(),
                record.offset(),
                record.key(),
                exception.getClass().getSimpleName(),
                exception.getMessage(),
                cause.getClass().getSimpleName(),
                cause.getMessage()
        );
    }

    private Throwable rootCause(Throwable exception) {
        Throwable current = exception;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
}
