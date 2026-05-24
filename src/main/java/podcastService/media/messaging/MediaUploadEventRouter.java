package podcastService.media.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import podcastService.infrastructure.messaging.error.KafkaMessageValidationException;
import podcastService.infrastructure.messaging.kafka.KafkaRecordContext;
import podcastService.media.messaging.contract.MediaObjectType;
import podcastService.media.messaging.contract.MediaUploadEventDto;
import podcastService.media.messaging.contract.MediaUploadEventType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class MediaUploadEventRouter {

    private final Map<MediaHandlerKey<MediaUploadEventType>, MediaUploadEventHandler> handlers;

    public MediaUploadEventRouter(List<MediaUploadEventHandler> handlers) {
        Map<MediaHandlerKey<MediaUploadEventType>, MediaUploadEventHandler> registry = new HashMap<>();
        for (MediaUploadEventHandler handler : handlers) {
            MediaUploadEventHandler previous = registry.put(handler.key(), handler);
            if (previous != null) {
                throw new IllegalStateException("Duplicate media.upload handler for key: " + handler.key());
            }
        }
        this.handlers = Map.copyOf(registry);
    }

    public void route(MediaUploadEventDto event, KafkaRecordContext context) {
        MediaObjectType objectType = require(event.normalizedObjectType(), "object_type");
        MediaUploadEventType eventType = require(event.normalizedEvent(), "event");
        MediaUploadEventHandler handler = handlers.get(new MediaHandlerKey<>(objectType, eventType));
        if (handler == null) {
            log.warn(
                    "Unsupported media.upload event: objectType={}, event={}, objectId={}, podcastId={}, topic={}, partition={}, offset={}",
                    objectType, eventType, event.objectId(), event.podcastId(),
                    context.topic(), context.partition(), context.offset()
            );
            throw new KafkaMessageValidationException("Unsupported media.upload event: " + objectType + "/" + eventType);
        }
        handler.handle(event, context);
    }

    private <T> T require(T value, String field) {
        if (value == null) {
            throw new KafkaMessageValidationException("media.upload event has missing field: " + field);
        }
        return value;
    }
}
