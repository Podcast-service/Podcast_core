package podcastService.infrastructure.messaging.config;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.util.backoff.FixedBackOff;
import podcastService.infrastructure.messaging.error.InvalidKafkaMessageException;
import podcastService.infrastructure.messaging.error.KafkaDeserializationException;
import podcastService.infrastructure.messaging.error.KafkaExceptionLogger;
import podcastService.infrastructure.messaging.error.KafkaMessageValidationException;

@Configuration
@RequiredArgsConstructor
public class KafkaErrorHandlerConfig {

    private final KafkaMessagingProperties kafkaMessagingProperties;

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(
            KafkaOperations<Object, Object> kafkaTemplate,
            KafkaExceptionLogger kafkaExceptionLogger
    ) {
        DefaultErrorHandler errorHandler = getDefaultErrorHandler(kafkaTemplate, kafkaExceptionLogger, kafkaMessagingProperties);

        errorHandler.addNotRetryableExceptions(
                InvalidKafkaMessageException.class,
                KafkaDeserializationException.class,
                KafkaMessageValidationException.class
        );

        errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
            if (record != null) {
                kafkaExceptionLogger.logRetryAttempt(record, ex, deliveryAttempt);
            }
        });

        return errorHandler;
    }

    private static DefaultErrorHandler getDefaultErrorHandler(
            KafkaOperations<Object, Object> kafkaTemplate,
            KafkaExceptionLogger kafkaExceptionLogger,
            KafkaMessagingProperties kafkaMessagingProperties
    ) {
        DeadLetterPublishingRecoverer deadLetterRecoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (ConsumerRecord<?, ?> record, Exception exception) -> {
                    String dltTopic = record.topic() + kafkaMessagingProperties.getDlt().getSuffix();
                    kafkaExceptionLogger.logSentToDlt(record, exception, dltTopic);
                    return new TopicPartition(dltTopic, record.partition());
                }
        );

        ConsumerRecordRecoverer recover = (record, exception) -> {
            if (isInvalidContractException(exception)) {
                kafkaExceptionLogger.logSkippedInvalidMessage(record, exception);
                return;
            }
            deadLetterRecoverer.accept(record, exception);
        };

        return new DefaultErrorHandler(
                recover,
                new FixedBackOff(
                        kafkaMessagingProperties.getRetry().getBackoffMs(),
                        kafkaMessagingProperties.getRetry().getMaxAttempts()
                )
        );
    }

    private static boolean isInvalidContractException(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof InvalidKafkaMessageException
                    || current instanceof KafkaDeserializationException
                    || current instanceof KafkaMessageValidationException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
