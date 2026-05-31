package podcastService.media.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import podcastService.infrastructure.messaging.error.KafkaMessageValidationException;
import podcastService.infrastructure.messaging.kafka.KafkaMessageReader;
import podcastService.infrastructure.messaging.kafka.KafkaRecordContext;
import podcastService.media.messaging.contract.MediaErrorEventDto;
import podcastService.media.messaging.contract.MediaObjectType;
import podcastService.podcast.service.PodcastMediaMetadataService;

@Slf4j
@Service
@RequiredArgsConstructor
public class TtsFailedConsumer {

    private static final String ERROR_EVENT = "error";

    private final KafkaMessageReader messageReader;
    private final PodcastMediaMetadataService podcastMediaMetadataService;

    @KafkaListener(topics = "${app.kafka.topics.tts-failed}", groupId = "${spring.kafka.consumer.group-id}")
    public void onMessage(ConsumerRecord<String, String> record) {
        KafkaRecordContext context = KafkaRecordContext.from(record);
        MediaErrorEventDto event = messageReader.read(record.value(), MediaErrorEventDto.class, context);
        validate(event);
        log.info(
                "Kafka tts.failed event received: topic={}, partition={}, offset={}, objectType={}, objectId={}, correlationId={}, messageId={}",
                context.topic(), context.partition(), context.offset(), event.objectType(), event.objectId(),
                context.correlationId(), context.messageId()
        );
        podcastMediaMetadataService.markFailed(event.objectId(), event.error(), event.timestamp(), "tts.failed");
        log.info("Kafka tts.failed event processed: topic={}, partition={}, offset={}, objectId={}",
                context.topic(), context.partition(), context.offset(), event.objectId());
    }

    private void validate(MediaErrorEventDto event) {
        if (event.objectType() != MediaObjectType.PODCAST_FILE_URL) {
            throw new KafkaMessageValidationException("tts.failed event has unsupported object_type");
        }
        if (event.objectId() == null) {
            throw new KafkaMessageValidationException("tts.failed event has missing object_id");
        }
        if (!ERROR_EVENT.equals(event.event())) {
            throw new KafkaMessageValidationException("tts.failed event has unsupported event");
        }
        if (event.error() == null || event.error().isBlank()) {
            throw new KafkaMessageValidationException("tts.failed event has missing error");
        }
    }
}
