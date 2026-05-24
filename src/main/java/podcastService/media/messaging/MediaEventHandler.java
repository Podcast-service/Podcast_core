package podcastService.media.messaging;

import podcastService.infrastructure.messaging.kafka.KafkaRecordContext;

public interface MediaEventHandler {

    MediaEventKey key();

    void handle(MediaEvent event, KafkaRecordContext context);
}
