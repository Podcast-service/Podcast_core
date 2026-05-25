package podcastService.media.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import podcastService.infrastructure.messaging.kafka.KafkaMessageReader;
import podcastService.infrastructure.messaging.kafka.KafkaRecordContext;
import podcastService.media.messaging.contract.MediaUploadEventDto;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaUploadConsumer {

    private final KafkaMessageReader messageReader;
    private final MediaUploadEventRouter router;

    @KafkaListener(topics = "${app.kafka.topics.media-upload}", groupId = "${spring.kafka.consumer.group-id}")
    public void onMessage(ConsumerRecord<String, String> record) {
        KafkaRecordContext context = KafkaRecordContext.from(record);
        MediaUploadEventDto event = messageReader.read(record.value(), MediaUploadEventDto.class, context);
        log.info(
                "Kafka media.upload event received: topic={}, partition={}, offset={}, key={}, objectType={}, event={}, objectId={}, podcastId={}, correlationId={}, messageId={}",
                context.topic(), context.partition(), context.offset(), context.key(), event.normalizedObjectType(),
                event.normalizedEvent(), event.objectId(), event.podcastId(), context.correlationId(), context.messageId()
        );
        router.route(event, context);
        log.info(
                "Kafka media.upload event processed: topic={}, partition={}, offset={}, objectType={}, event={}, objectId={}, podcastId={}",
                context.topic(), context.partition(), context.offset(), event.normalizedObjectType(),
                event.normalizedEvent(), event.objectId(), event.podcastId()
        );
    }
}
