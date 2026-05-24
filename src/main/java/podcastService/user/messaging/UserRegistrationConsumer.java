package podcastService.user.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import podcastService.infrastructure.messaging.kafka.KafkaJsonMessageParser;
import podcastService.infrastructure.messaging.kafka.KafkaRecordContext;
import podcastService.user.service.UserProfileService;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRegistrationConsumer {

    private final KafkaJsonMessageParser messageParser;
    private final UserRegisteredEventParser eventParser;
    private final UserProfileService userProfileService;

    @KafkaListener(
            topics = "${app.kafka.topics.user-register}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onMessage(ConsumerRecord<String, String> record) {
        KafkaRecordContext context = KafkaRecordContext.from(record);
        JsonNode payload = messageParser.parse(record.value(), context);
        UserRegisteredEvent event = eventParser.parse(payload);

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
}
