package podcastService.subscription.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import podcastService.author.entity.AuthorEntity;
import podcastService.author.repository.AuthorRepository;
import podcastService.common.exception.BusinessRuleException;
import podcastService.podcast.mapper.PodcastMapper;
import podcastService.podcast.repository.PodcastRepository;
import podcastService.podcast.repository.PodcastVoteRepository;
import podcastService.subscription.dto.AuthorSubscriptionResponse;
import podcastService.subscription.entity.SubscriptionEntity;
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
class SubscriptionServiceTest {

    private static final UUID USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID PROFILE_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final UUID AUTHOR_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private AuthorRepository authorRepository;
    @Mock private PodcastRepository podcastRepository;
    @Mock private PodcastVoteRepository podcastVoteRepository;
    @Mock private PodcastMapper podcastMapper;
    private SubscriptionService service;

    @BeforeEach
    void setUp() {
        service = new SubscriptionService(
                subscriptionRepository,
                userProfileRepository,
                authorRepository,
                podcastRepository,
                podcastVoteRepository,
                podcastMapper
        );
    }

    @Test
    void subscribeCreatesSubscriptionAndIncrementsCounter() {
        UserProfileEntity user = userProfile(PROFILE_ID, USER_ID);
        AuthorEntity author = author(AUTHOR_ID, userProfile(UUID.fromString("550e8400-e29b-41d4-a716-446655440101"), UUID.randomUUID()));
        author.setSubscribersCount(2);

        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));
        when(authorRepository.findDetailedByIdForUpdate(AUTHOR_ID)).thenReturn(Optional.of(author));
        when(subscriptionRepository.existsByIdSubscriberProfileIdAndIdAuthorId(PROFILE_ID, AUTHOR_ID)).thenReturn(false);

        AuthorSubscriptionResponse response = service.subscribe(AUTHOR_ID, USER_ID);

        assertThat(author.getSubscribersCount()).isEqualTo(3);
        assertThat(response.isSubscribed()).isTrue();
        assertThat(response.subscribersCount()).isEqualTo(3);
        verify(subscriptionRepository).saveAndFlush(any(SubscriptionEntity.class));
        verify(authorRepository).saveAndFlush(author);
    }

    @Test
    void subscribeIsIdempotentWhenSubscriptionAlreadyExists() {
        UserProfileEntity user = userProfile(PROFILE_ID, USER_ID);
        AuthorEntity author = author(AUTHOR_ID, userProfile(UUID.fromString("550e8400-e29b-41d4-a716-446655440101"), UUID.randomUUID()));
        author.setSubscribersCount(2);

        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));
        when(authorRepository.findDetailedByIdForUpdate(AUTHOR_ID)).thenReturn(Optional.of(author));
        when(subscriptionRepository.existsByIdSubscriberProfileIdAndIdAuthorId(PROFILE_ID, AUTHOR_ID)).thenReturn(true);

        AuthorSubscriptionResponse response = service.subscribe(AUTHOR_ID, USER_ID);

        assertThat(response.subscribersCount()).isEqualTo(2);
        verify(subscriptionRepository, never()).saveAndFlush(any());
        verify(authorRepository, never()).saveAndFlush(any());
    }

    @Test
    void subscribeRejectsOwnAuthorProfile() {
        UserProfileEntity user = userProfile(PROFILE_ID, USER_ID);
        AuthorEntity author = author(AUTHOR_ID, user);

        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));
        when(authorRepository.findDetailedByIdForUpdate(AUTHOR_ID)).thenReturn(Optional.of(author));

        assertThatThrownBy(() -> service.subscribe(AUTHOR_ID, USER_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("You cannot subscribe to your own author profile");
    }

    private UserProfileEntity userProfile(UUID profileId, UUID userId) {
        UserProfileEntity user = new UserProfileEntity();
        user.setId(profileId);
        user.setUserId(userId);
        user.setUsername("user-" + profileId);
        return user;
    }

    private AuthorEntity author(UUID authorId, UserProfileEntity owner) {
        AuthorEntity author = new AuthorEntity();
        author.setId(authorId);
        author.setUserProfile(owner);
        author.setAuthorName("Author");
        return author;
    }
}
