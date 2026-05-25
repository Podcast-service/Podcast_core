package podcastService.media.messaging;

import podcastService.infrastructure.messaging.kafka.KafkaRecordContext;
import podcastService.media.messaging.contract.MediaUploadEventDto;
import podcastService.media.messaging.contract.MediaUploadEventType;

public interface MediaUploadEventHandler {

    MediaHandlerKey<MediaUploadEventType> key();

    void handle(MediaUploadEventDto event, KafkaRecordContext context);
}
