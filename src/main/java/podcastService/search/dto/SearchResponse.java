package podcastService.search.dto;

import podcastService.author.dto.AuthorCard;
import podcastService.common.dto.PageResponse;
import podcastService.playlist.dto.PlaylistCard;
import podcastService.podcast.dto.PodcastCard;

public record SearchResponse(
        PageResponse<PodcastCard> podcasts,
        PageResponse<AuthorCard> authors,
        PageResponse<PlaylistCard> playlists
) {
}
