package podcastService.media.messaging.handler;

import lombok.extern.slf4j.Slf4j;
import podcastService.infrastructure.messaging.kafka.KafkaRecordContext;
import podcastService.media.messaging.MediaEvent;
import podcastService.media.messaging.MediaEventHandler;
import podcastService.media.messaging.MediaEventKey;

@Slf4j
public abstract class MediaNoopHandler implements MediaEventHandler {

    private final MediaEventKey key;

    protected MediaNoopHandler(String type, String event) {
        this.key = new MediaEventKey(type, event);
    }

    @Override
    public MediaEventKey key() {
        return key;
    }

    @Override
    public void handle(MediaEvent event, KafkaRecordContext context) {
        log.info(
                "Media event accepted without state change: type={}, event={}, objectId={}, topic={}, partition={}, offset={}",
                event.type(),
                event.event(),
                event.objectId(),
                context.topic(),
                context.partition(),
                context.offset()
        );
    }
}
