package podcastService.infrastructure.messaging.error;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class KafkaExceptionLogger {

    private static final Logger log = LoggerFactory.getLogger(KafkaExceptionLogger.class);

    public void logRetryAttempt(ConsumerRecord<?, ?> record, Exception exception, int deliveryAttempt) {
        log.warn(
                "Kafka processing failed, will retry. topic={}, partition={}, offset={}, key={}, deliveryAttempt={}, exception={}, message={}",
                record.topic(),
                record.partition(),
                record.offset(),
                record.key(),
                deliveryAttempt,
                exception.getClass().getSimpleName(),
                exception.getMessage()
        );
    }

    public void logSentToDlt(ConsumerRecord<?, ?> record, Exception exception, String dltTopic) {
        log.error(
                "Kafka message redirected to DLT. sourceTopic={}, dltTopic={}, partition={}, offset={}, key={}, exception={}, message={}",
                record.topic(),
                dltTopic,
                record.partition(),
                record.offset(),
                record.key(),
                exception.getClass().getSimpleName(),
                exception.getMessage(),
                exception
        );
    }

    public void logSkippedInvalidMessage(ConsumerRecord<?, ?> record, Exception exception) {
        log.warn(
                "Kafka message skipped because it does not match expected contract. topic={}, partition={}, offset={}, key={}, exception={}, message={}",
                record.topic(),
                record.partition(),
                record.offset(),
                record.key(),
                exception.getClass().getSimpleName(),
                exception.getMessage()
        );
    }
}
