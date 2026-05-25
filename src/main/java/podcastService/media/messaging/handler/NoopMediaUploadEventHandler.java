package podcastService.media.messaging.handler;

import lombok.extern.slf4j.Slf4j;
import podcastService.infrastructure.messaging.kafka.KafkaRecordContext;
import podcastService.media.messaging.MediaHandlerKey;
import podcastService.media.messaging.MediaUploadEventHandler;
import podcastService.media.messaging.contract.MediaObjectType;
import podcastService.media.messaging.contract.MediaUploadEventDto;
import podcastService.media.messaging.contract.MediaUploadEventType;

@Slf4j
public abstract class NoopMediaUploadEventHandler implements MediaUploadEventHandler {

    private final MediaHandlerKey<MediaUploadEventType> key;

    protected NoopMediaUploadEventHandler(MediaObjectType objectType, MediaUploadEventType event) {
        this.key = new MediaHandlerKey<>(objectType, event);
    }

    @Override
    public MediaHandlerKey<MediaUploadEventType> key() {
        return key;
    }

    @Override
    public void handle(MediaUploadEventDto event, KafkaRecordContext context) {
        log.info("media.upload event accepted without state change, objectType={}, event={}, objectId={}, topic={}, partition={}, offset={}",
                event.normalizedObjectType(), event.normalizedEvent(), event.objectId(),
                context.topic(), context.partition(), context.offset());
    }
}
