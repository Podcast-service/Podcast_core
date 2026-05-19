package podcastService.user.messaging.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import podcastService.infrastructure.messaging.error.InvalidKafkaMessageException;
import podcastService.infrastructure.messaging.error.KafkaDeserializationException;
import podcastService.infrastructure.messaging.event.EventEnvelope;
import podcastService.user.dto.CreateUserRequest;
import podcastService.user.service.UserProfileService;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileEventHandler {
    private final ObjectMapper objectMapper;
    private final UserProfileService userProfileService;

    public void handle(EventEnvelope event) {
        validateEnvelope(event);

        switch (event.eventType()) {
            case USER_CREATED -> {
                log.info("Processed event: {}", event.eventType());
                handleUserCreated(event);
            }
            default -> throw new InvalidKafkaMessageException(
                    "Unsupported event type for users topic: " + event.eventType()
            );
        }
    }

    private void handleUserCreated(EventEnvelope event) {
        CreateUserRequest request = convertPayload(event, CreateUserRequest.class);

        if (request.userId() == null) {
            throw new InvalidKafkaMessageException("USER_CREATED payload has null userId");
        }
        if (request.username() == null || request.username().isBlank()) {
            throw new InvalidKafkaMessageException("USER_CREATED payload has blank username");
        }

        userProfileService.createNewUser(request);

        log.info("Processed USER_CREATED event successfully: userId={}",
                request.userId());
    }

    private void validateEnvelope(EventEnvelope event) {
        if (event == null) {
            throw  new InvalidKafkaMessageException("Kafka event is null");
        }
        if (event.eventType() == null) {
            throw new InvalidKafkaMessageException("Kafka eventType is null");
        }
        if (event.occurredAt() == null) {
            throw new InvalidKafkaMessageException("Kafka occurredAt is null");
        }
        if (event.payload() == null || event.payload().isNull()) {
            throw new InvalidKafkaMessageException("Kafka payload is null");
        }
    }

    private <T> T convertPayload(EventEnvelope event, Class<T> tClass) {
        try {
            return objectMapper.convertValue(event.payload(), tClass);
        } catch (IllegalArgumentException exception) {
            throw new KafkaDeserializationException(
                    "Failed to deserialize payload for eventType=" + event.eventType() +
                            " to " + tClass.getSimpleName(),
                    exception
            );
        }
    }
}
