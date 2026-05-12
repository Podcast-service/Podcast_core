package podcastService.search.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import podcastService.author.repository.AuthorRepository;
import podcastService.common.exception.BadRequestException;
import podcastService.playlist.mapper.PlaylistMapper;
import podcastService.playlist.repository.PlaylistRepository;
import podcastService.podcast.mapper.PodcastMapper;
import podcastService.podcast.repository.PodcastRepository;
import podcastService.podcast.repository.PodcastVoteRepository;
import podcastService.search.repository.SearchRepository;
import podcastService.subscription.repository.SubscriptionRepository;
import podcastService.user.repository.UserProfileRepository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SearchServiceTest {

    private SearchService service;

    @BeforeEach
    void setUp() {
        service = new SearchService(
                Mockito.mock(SearchRepository.class),
                Mockito.mock(PodcastRepository.class),
                Mockito.mock(AuthorRepository.class),
                Mockito.mock(PlaylistRepository.class),
                Mockito.mock(PodcastVoteRepository.class),
                Mockito.mock(SubscriptionRepository.class),
                Mockito.mock(UserProfileRepository.class),
                Mockito.mock(PodcastMapper.class),
                Mockito.mock(PlaylistMapper.class)
        );
    }

    @Test
    void suggestRejectsBlankQuery() {
        assertThatThrownBy(() -> service.suggest("   "))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Search query must not be blank");
    }
}
