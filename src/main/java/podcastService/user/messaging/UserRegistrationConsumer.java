package podcastService.user.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import podcastService.infrastructure.messaging.error.KafkaMessageValidationException;
import podcastService.infrastructure.messaging.kafka.KafkaMessageReader;
import podcastService.infrastructure.messaging.kafka.KafkaRecordContext;
import podcastService.user.service.UserProfileService;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRegistrationConsumer {

    private final KafkaMessageReader messageReader;
    private final UserProfileService userProfileService;

    @KafkaListener(
            topics = "${app.kafka.topics.user-register}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onMessage(ConsumerRecord<String, String> record) {
        KafkaRecordContext context = KafkaRecordContext.from(record);
        UserRegisteredEvent event = messageReader.read(record.value(), UserRegisteredEvent.class, context);
        validate(event);

        log.info(
                "Kafka user registration event received: topic={}, partition={}, offset={}, key={}, userId={}, correlationId={}, messageId={}",
                context.topic(),
                context.partition(),
                context.offset(),
                context.key(),
                event.userId(),
                context.correlationId(),
                context.messageId()
        );

        userProfileService.upsertFromKafkaRegistration(event.userId(), event.username());

        log.info(
                "Kafka user registration event processed: topic={}, partition={}, offset={}, userId={}",
                context.topic(),
                context.partition(),
                context.offset(),
                event.userId()
        );
    }

    private void validate(UserRegisteredEvent event) {
        if (event.userId() == null) {
            throw new KafkaMessageValidationException("podcast.user.register event has missing user_id");
        }
        if (event.username() == null || event.username().isBlank()) {
            throw new KafkaMessageValidationException("podcast.user.register event has missing username");
        }
    }
}
