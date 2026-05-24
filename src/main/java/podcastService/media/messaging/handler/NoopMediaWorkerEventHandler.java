package podcastService.media.messaging.handler;

import lombok.extern.slf4j.Slf4j;
import podcastService.infrastructure.messaging.kafka.KafkaRecordContext;
import podcastService.media.messaging.MediaHandlerKey;
import podcastService.media.messaging.MediaWorkerEventHandler;
import podcastService.media.messaging.contract.MediaObjectType;
import podcastService.media.messaging.contract.MediaWorkerEventDto;
import podcastService.media.messaging.contract.MediaWorkerEventType;

@Slf4j
public abstract class NoopMediaWorkerEventHandler implements MediaWorkerEventHandler {

    private final MediaHandlerKey<MediaWorkerEventType> key;

    protected NoopMediaWorkerEventHandler(MediaObjectType objectType, MediaWorkerEventType event) {
        this.key = new MediaHandlerKey<>(objectType, event);
    }

    @Override
    public MediaHandlerKey<MediaWorkerEventType> key() {
        return key;
    }

    @Override
    public void handle(MediaWorkerEventDto event, KafkaRecordContext context) {
        log.info("media.worker event accepted without state change, objectType={}, event={}, objectId={}, topic={}, partition={}, offset={}",
                event.normalizedObjectType(), event.normalizedEvent(), event.objectId(),
                context.topic(), context.partition(), context.offset());
    }
}
