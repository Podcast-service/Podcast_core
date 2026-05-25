package podcastService.media.messaging;

import podcastService.infrastructure.messaging.kafka.KafkaRecordContext;
import podcastService.media.messaging.contract.MediaWorkerEventDto;
import podcastService.media.messaging.contract.MediaWorkerEventType;

public interface MediaWorkerEventHandler {

    MediaHandlerKey<MediaWorkerEventType> key();

    void handle(MediaWorkerEventDto event, KafkaRecordContext context);
}
