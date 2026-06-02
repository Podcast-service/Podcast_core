package podcastService.media.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import podcastService.infrastructure.messaging.error.KafkaMessageValidationException;
import podcastService.infrastructure.messaging.kafka.KafkaMessageReader;
import podcastService.infrastructure.messaging.kafka.KafkaRecordContext;
import podcastService.media.messaging.contract.MediaSubtitleEventDto;
import podcastService.podcast.service.PodcastMediaMetadataService;
import podcastService.transcript.service.SubtitleTranscriptService;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaSubtitleConsumer {

    private final KafkaMessageReader messageReader;
    private final PodcastMediaMetadataService podcastMediaMetadataService;
    private final SubtitleTranscriptService subtitleTranscriptService;

    @KafkaListener(topics = "${app.kafka.topics.media-subtitle}", groupId = "${spring.kafka.consumer.group-id}")
    public void onMessage(ConsumerRecord<String, String> record) {
        KafkaRecordContext context = KafkaRecordContext.from(record);
        MediaSubtitleEventDto event = messageReader.read(record.value(), MediaSubtitleEventDto.class, context);
        validate(event);
        log.info("Kafka media.subtitle event received: topic={}, partition={}, offset={}, podcastId={}, correlationId={}, messageId={}",
                context.topic(), context.partition(), context.offset(), event.podcastId(), context.correlationId(), context.messageId());
        podcastMediaMetadataService.saveTranscriptContent(
                event.podcastId(),
                subtitleTranscriptService.buildSpeakerBlocks(event.content()),
                event.readyAt(),
                "media.subtitle"
        );
        log.info("Kafka media.subtitle event processed: topic={}, partition={}, offset={}, podcastId={}",
                context.topic(), context.partition(), context.offset(), event.podcastId());
    }

    private void validate(MediaSubtitleEventDto event) {
        if (event.podcastId() == null) {
            throw new KafkaMessageValidationException("media.subtitle event has missing podcast_id");
        }
        if (event.content() == null) {
            throw new KafkaMessageValidationException("media.subtitle event has missing content");
        }
    }
}
