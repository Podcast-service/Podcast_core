package podcastService.media.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import podcastService.infrastructure.messaging.error.KafkaMessageValidationException;
import podcastService.infrastructure.messaging.kafka.KafkaRecordContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class MediaEventRouter {

    private final Map<MediaEventKey, MediaEventHandler> handlers;

    public MediaEventRouter(List<MediaEventHandler> handlers) {
        Map<MediaEventKey, MediaEventHandler> registry = new HashMap<>();
        for (MediaEventHandler handler : handlers) {
            MediaEventHandler previous = registry.put(handler.key(), handler);
            if (previous != null) {
                throw new IllegalStateException("Duplicate media event handler for key: " + handler.key());
            }
        }
        this.handlers = Map.copyOf(registry);
    }

    public void route(MediaEvent event, KafkaRecordContext context) {
        MediaEventHandler handler = handlers.get(event.key());
        if (handler == null) {
            log.warn(
                    "Unsupported media event received: type={}, event={}, objectId={}, topic={}, partition={}, offset={}, correlationId={}, messageId={}",
                    event.type(),
                    event.event(),
                    event.objectId(),
                    context.topic(),
                    context.partition(),
                    context.offset(),
                    context.correlationId(),
                    context.messageId()
            );
            throw new KafkaMessageValidationException("Unsupported media event: " + event.type() + "/" + event.event());
        }

        handler.handle(event, context);
    }
}
