package podcastService.user.messaging.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import podcastService.user.dto.CreateUserRequest;
import podcastService.user.service.UserProfileService;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserProfileConsumer {
    private final UserProfileService userProfileService;

    @KafkaListener(
            topics = "${app.kafka.topics.user.created}",
            groupId = "${spring.kafka.consumer.group-id}")
    public void onUserCreated(CreateUserRequest request) {
        log.info("Received Kafka user.created event: userId={}, username={}",
                request != null ? request.userId() : null,
                request != null ? request.username() : null);

        userProfileService.createNewUser(request);

        log.info("Processed Kafka user.created event successfully: userId={}",
                request != null ? request.userId() : null);
    }
}
