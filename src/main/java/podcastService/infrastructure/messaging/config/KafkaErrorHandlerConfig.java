package podcastService.infrastructure.messaging.config;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.util.backoff.FixedBackOff;
import podcastService.infrastructure.messaging.error.InvalidKafkaMessageException;
import podcastService.infrastructure.messaging.error.KafkaDeserializationException;
import podcastService.infrastructure.messaging.error.KafkaExceptionLogger;
import podcastService.infrastructure.messaging.error.KafkaMessageValidationException;

@Configuration
public class KafkaErrorHandlerConfig {

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(
            KafkaOperations<Object, Object> kafkaTemplate,
            KafkaExceptionLogger kafkaExceptionLogger
    ) {
        DefaultErrorHandler errorHandler = getDefaultErrorHandler(kafkaTemplate, kafkaExceptionLogger);

        errorHandler.addNotRetryableExceptions(
                InvalidKafkaMessageException.class,
                KafkaDeserializationException.class,
                KafkaMessageValidationException.class,
                IllegalArgumentException.class
        );

        errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
            if (record != null) {
                kafkaExceptionLogger.logRetryAttempt(record, ex, deliveryAttempt);
            }
        });

        return errorHandler;
    }

    private static DefaultErrorHandler getDefaultErrorHandler(KafkaOperations<Object, Object> kafkaTemplate, KafkaExceptionLogger kafkaExceptionLogger) {
        DeadLetterPublishingRecoverer recover = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (ConsumerRecord<?, ?> record, Exception exception) -> {
                    String dltTopic = record.topic() + ".DLT";
                    kafkaExceptionLogger.logSentToDlt(record, exception, dltTopic);
                    return new TopicPartition(dltTopic, record.partition());
                }
        );

        return new DefaultErrorHandler(
                recover,
                new FixedBackOff(1000L, 3L)
        );
    }
}
