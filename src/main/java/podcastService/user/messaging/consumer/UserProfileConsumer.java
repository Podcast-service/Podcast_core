package podcastService.user.messaging.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import podcastService.infrastructure.messaging.event.EventEnvelope;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserProfileConsumer {
    private final UserProfileEventHandler eventHandler;

    @KafkaListener(
            topics = "${app.kafka.topics.users}",
            groupId = "${spring.kafka.consumer.group-id}")
    public void onUserEvent(EventEnvelope event) {
        eventHandler.handle(event);
    }
}
