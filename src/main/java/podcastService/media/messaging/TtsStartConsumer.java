package podcastService.media.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import podcastService.infrastructure.messaging.error.KafkaMessageValidationException;
import podcastService.infrastructure.messaging.kafka.KafkaMessageReader;
import podcastService.infrastructure.messaging.kafka.KafkaRecordContext;
import podcastService.media.messaging.contract.TtsStartEventDto;
import podcastService.podcast.service.PodcastMediaMetadataService;

@Slf4j
@Service
@RequiredArgsConstructor
public class TtsStartConsumer {

    private final KafkaMessageReader messageReader;
    private final PodcastMediaMetadataService podcastMediaMetadataService;

    @KafkaListener(topics = "${app.kafka.topics.tts-start}", groupId = "${spring.kafka.consumer.group-id}")
    public void onMessage(ConsumerRecord<String, String> record) {
        KafkaRecordContext context = KafkaRecordContext.from(record);
        TtsStartEventDto event = messageReader.read(record.value(), TtsStartEventDto.class, context);
        validate(event);
        log.info("Kafka tts.start event received: topic={}, partition={}, offset={}, podcastId={}, correlationId={}, messageId={}",
                context.topic(), context.partition(), context.offset(), event.podcastId(), context.correlationId(), context.messageId());
        podcastMediaMetadataService.saveTtsContent(event.podcastId(), event.content(), event.timestamp());
        log.info("Kafka tts.start event processed: topic={}, partition={}, offset={}, podcastId={}",
                context.topic(), context.partition(), context.offset(), event.podcastId());
    }

    private void validate(TtsStartEventDto event) {
        if (event.podcastId() == null) {
            throw new KafkaMessageValidationException("tts.start event has missing podcast_id");
        }
        if (event.content() == null || event.content().isBlank()) {
            throw new KafkaMessageValidationException("tts.start event has missing content");
        }
    }
}
