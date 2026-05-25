package podcastService.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import podcastService.infrastructure.messaging.error.KafkaRetryableProcessingException;
import podcastService.user.mapper.UserProfileMapper;
import podcastService.user.repository.UserProfileRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceKafkaTest {

    private static final UUID USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserProfileMapper userProfileMapper;

    private UserProfileService service;

    @BeforeEach
    void setUp() {
        service = new UserProfileService(userProfileRepository, userProfileMapper);
    }

    @Test
    void upsertFromKafkaRegistrationDelegatesToRepository() {
        service.upsertFromKafkaRegistration(USER_ID, " testuser ");

        verify(userProfileRepository).upsertUserProfile(USER_ID, "testuser");
    }

    @Test
    void avatarUpdateRetriesWhenProfileIsNotFound() {
        when(userProfileRepository.updateAvatarByProfileIdOrUserId(USER_ID, "/avatars/u.png")).thenReturn(0);

        assertThatThrownBy(() -> service.updateAvatarFromMediaEvent(USER_ID, "/avatars/u.png"))
                .isInstanceOf(KafkaRetryableProcessingException.class);
    }
}
