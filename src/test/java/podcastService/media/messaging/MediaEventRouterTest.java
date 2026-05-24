package podcastService.media.messaging;

import org.junit.jupiter.api.Test;
import podcastService.infrastructure.messaging.error.KafkaMessageValidationException;
import podcastService.infrastructure.messaging.kafka.KafkaRecordContext;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MediaEventRouterTest {

    private static final KafkaRecordContext CONTEXT = new KafkaRecordContext(
            "media", 0, 10L, "key", "corr", "msg"
    );

    @Test
    void routesToRegisteredHandler() {
        AtomicBoolean called = new AtomicBoolean(false);
        MediaEventHandler handler = new MediaEventHandler() {
            @Override
            public MediaEventKey key() {
                return new MediaEventKey("podcast_file", "uploaded");
            }

            @Override
            public void handle(MediaEvent event, KafkaRecordContext context) {
                called.set(true);
            }
        };

        MediaEventRouter router = new MediaEventRouter(List.of(handler));
        router.route(new MediaEvent("podcast_file", "uploaded", UUID.randomUUID(), null), CONTEXT);

        assertThat(called).isTrue();
    }

    @Test
    void rejectsUnknownEventForDltProcessing() {
        MediaEventRouter router = new MediaEventRouter(List.of());

        assertThatThrownBy(() -> router.route(
                new MediaEvent("unknown", "uploaded", UUID.randomUUID(), null),
                CONTEXT
        )).isInstanceOf(KafkaMessageValidationException.class);
    }
}
