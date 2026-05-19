package podcastService.author.dto;

import java.util.UUID;

public record AuthorCard(
        UUID id,
        String authorName,
        String avatarUrl,
        Long subscribersCount,
        Boolean isSubscribed
) {
}
