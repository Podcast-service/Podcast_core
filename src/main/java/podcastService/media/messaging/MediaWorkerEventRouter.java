package podcastService.media.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import podcastService.infrastructure.messaging.error.KafkaMessageValidationException;
import podcastService.infrastructure.messaging.kafka.KafkaRecordContext;
import podcastService.media.messaging.contract.MediaObjectType;
import podcastService.media.messaging.contract.MediaWorkerEventDto;
import podcastService.media.messaging.contract.MediaWorkerEventType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class MediaWorkerEventRouter {

    private final Map<MediaHandlerKey<MediaWorkerEventType>, MediaWorkerEventHandler> handlers;

    public MediaWorkerEventRouter(List<MediaWorkerEventHandler> handlers) {
        Map<MediaHandlerKey<MediaWorkerEventType>, MediaWorkerEventHandler> registry = new HashMap<>();
        for (MediaWorkerEventHandler handler : handlers) {
            MediaWorkerEventHandler previous = registry.put(handler.key(), handler);
            if (previous != null) {
                throw new IllegalStateException("Duplicate media.worker handler for key: " + handler.key());
            }
        }
        this.handlers = Map.copyOf(registry);
    }

    public void route(MediaWorkerEventDto event, KafkaRecordContext context) {
        MediaObjectType objectType = require(event.normalizedObjectType(), "object_type");
        MediaWorkerEventType eventType = require(event.normalizedEvent(), "event");
        MediaWorkerEventHandler handler = handlers.get(new MediaHandlerKey<>(objectType, eventType));
        if (handler == null) {
            log.warn(
                    "Unsupported media.worker event: objectType={}, event={}, objectId={}, podcastId={}, topic={}, partition={}, offset={}",
                    objectType, eventType, event.objectId(), event.podcastId(),
                    context.topic(), context.partition(), context.offset()
            );
            throw new KafkaMessageValidationException("Unsupported media.worker event: " + objectType + "/" + eventType);
        }
        handler.handle(event, context);
    }

    private <T> T require(T value, String field) {
        if (value == null) {
            throw new KafkaMessageValidationException("media.worker event has missing field: " + field);
        }
        return value;
    }
}
