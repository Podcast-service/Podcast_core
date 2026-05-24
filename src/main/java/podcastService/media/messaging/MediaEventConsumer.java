package podcastService.media.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import podcastService.infrastructure.messaging.kafka.KafkaJsonMessageParser;
import podcastService.infrastructure.messaging.kafka.KafkaRecordContext;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaEventConsumer {

    private final KafkaJsonMessageParser messageParser;
    private final MediaEventParser eventParser;
    private final MediaEventRouter router;

    @KafkaListener(
            topics = "${app.kafka.topics.media}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onMessage(ConsumerRecord<String, String> record) {
        KafkaRecordContext context = KafkaRecordContext.from(record);
        JsonNode payload = messageParser.parse(record.value(), context);
        MediaEvent event = eventParser.parse(payload);

        log.info(
                "Kafka media event received: topic={}, partition={}, offset={}, key={}, type={}, event={}, objectId={}, correlationId={}, messageId={}",
                context.topic(),
                context.partition(),
                context.offset(),
                context.key(),
                event.type(),
                event.event(),
                event.objectId(),
                context.correlationId(),
                context.messageId()
        );

        router.route(event, context);

        log.info(
                "Kafka media event processed: topic={}, partition={}, offset={}, type={}, event={}, objectId={}",
                context.topic(),
                context.partition(),
                context.offset(),
                event.type(),
                event.event(),
                event.objectId()
        );
    }
}
