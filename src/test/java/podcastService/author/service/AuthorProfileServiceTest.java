package podcastService.author.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import podcastService.author.dto.AuthorProfileResponse;
import podcastService.author.dto.CreateAuthorProfileRequest;
import podcastService.author.dto.UpdateAuthorProfileRequest;
import podcastService.author.entity.AuthorEntity;
import podcastService.author.mapper.AuthorMapper;
import podcastService.author.repository.AuthorRepository;
import podcastService.common.exception.BadRequestException;
import podcastService.common.exception.ConflictException;
import podcastService.subscription.repository.SubscriptionRepository;
import podcastService.user.entity.UserProfileEntity;
import podcastService.user.repository.UserProfileRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorProfileServiceTest {

    private static final UUID USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID PROFILE_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final UUID AUTHOR_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    private AuthorProfileService service;

    @BeforeEach
    void setUp() {
        service = new AuthorProfileService(
                authorRepository,
                userProfileRepository,
                subscriptionRepository,
                new AuthorMapper()
        );
    }

    @Test
    void createTrimsTextAndCreatesAuthorForCurrentUser() {
        UserProfileEntity user = userProfile();
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));
        when(authorRepository.existsByUserProfileId(PROFILE_ID)).thenReturn(false);
        when(authorRepository.saveAndFlush(any(AuthorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthorProfileResponse response = service.create(
                USER_ID,
                new CreateAuthorProfileRequest("  Иван Петров  ", "  Технологии без тумана  ")
        );

        ArgumentCaptor<AuthorEntity> authorCaptor = ArgumentCaptor.forClass(AuthorEntity.class);
        verify(authorRepository).saveAndFlush(authorCaptor.capture());
        AuthorEntity savedAuthor = authorCaptor.getValue();

        assertThat(savedAuthor.getUserProfile()).isSameAs(user);
        assertThat(savedAuthor.getAuthorName()).isEqualTo("Иван Петров");
        assertThat(savedAuthor.getDescription()).isEqualTo("Технологии без тумана");
        assertThat(response.authorName()).isEqualTo("Иван Петров");
        assertThat(response.isSubscribed()).isFalse();
    }

    @Test
    void createRejectsDuplicateAuthorProfile() {
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(userProfile()));
        when(authorRepository.existsByUserProfileId(PROFILE_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.create(USER_ID, new CreateAuthorProfileRequest("Иван Петров", null)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Author profile already exists");

        verify(authorRepository, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsAuthorNameThatIsTooShortAfterTrim() {
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(userProfile()));
        when(authorRepository.existsByUserProfileId(PROFILE_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.create(USER_ID, new CreateAuthorProfileRequest(" a ", null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Request validation failed");

        verify(authorRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateRejectsEmptyPatch() {
        UpdateAuthorProfileRequest request = new UpdateAuthorProfileRequest();

        assertThatThrownBy(() -> service.updateMine(USER_ID, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("At least one field must be provided for update");
    }

    @Test
    void getPublicReturnsNullSubscriptionStatusForAnonymousUser() {
        AuthorEntity author = author();
        when(authorRepository.findDetailedById(AUTHOR_ID)).thenReturn(Optional.of(author));

        AuthorProfileResponse response = service.getPublic(AUTHOR_ID, null);

        assertThat(response.id()).isEqualTo(AUTHOR_ID);
        assertThat(response.isSubscribed()).isNull();
        verify(subscriptionRepository, never()).existsByIdSubscriberProfileIdAndIdAuthorId(any(), any());
    }

    @Test
    void getPublicReturnsSubscriptionStatusForAuthenticatedUser() {
        AuthorEntity author = author();
        UserProfileEntity currentUser = userProfile();
        currentUser.setId(UUID.fromString("550e8400-e29b-41d4-a716-446655440003"));

        when(authorRepository.findDetailedById(AUTHOR_ID)).thenReturn(Optional.of(author));
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(currentUser));
        when(subscriptionRepository.existsByIdSubscriberProfileIdAndIdAuthorId(currentUser.getId(), AUTHOR_ID))
                .thenReturn(true);

        AuthorProfileResponse response = service.getPublic(AUTHOR_ID, USER_ID);

        assertThat(response.isSubscribed()).isTrue();
    }

    private UserProfileEntity userProfile() {
        UserProfileEntity user = new UserProfileEntity();
        user.setId(PROFILE_ID);
        user.setUserId(USER_ID);
        user.setUsername("ivan");
        user.setAvatarUrl("https://example.com/avatar.png");
        return user;
    }

    private AuthorEntity author() {
        AuthorEntity author = new AuthorEntity();
        author.setId(AUTHOR_ID);
        author.setUserProfile(userProfile());
        author.setAuthorName("Иван Петров");
        author.setDescription("Про backend");
        author.setSubscribersCount(42);
        return author;
    }
}
