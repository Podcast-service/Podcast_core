package podcastService.user.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import podcastService.infrastructure.messaging.error.KafkaMessageValidationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserRegisteredEventParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UserRegisteredEventParser parser = new UserRegisteredEventParser();

    @Test
    void parsesUserRegisterEvent() throws Exception {
        UserRegisteredEvent event = parser.parse(objectMapper.readTree("""
                {
                  "user_id": "550e8400-e29b-41d4-a716-446655440000",
                  "username": "testuser"
                }
                """));

        assertThat(event.userId()).isEqualTo(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        assertThat(event.username()).isEqualTo("testuser");
    }

    @Test
    void rejectsBlankUsername() throws Exception {
        assertThatThrownBy(() -> parser.parse(objectMapper.readTree("""
                {
                  "user_id": "550e8400-e29b-41d4-a716-446655440000",
                  "username": " "
                }
                """)))
                .isInstanceOf(KafkaMessageValidationException.class);
    }
}
