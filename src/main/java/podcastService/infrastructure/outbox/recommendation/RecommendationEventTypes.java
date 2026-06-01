package podcastService.infrastructure.outbox.recommendation;

public final class RecommendationEventTypes {

    public static final String PODCAST_PUBLISHED = "podcast.published.v1";
    public static final String PODCAST_UPDATED = "podcast.updated.v1";
    public static final String PODCAST_DELETED = "podcast.deleted.v1";
    public static final String PODCAST_PLAY_FINISHED = "podcast.play_finished.v1";
    public static final String PODCAST_LIKED = "podcast.liked.v1";
    public static final String PODCAST_DISLIKED = "podcast.disliked.v1";
    public static final String AUTHOR_FOLLOWED = "author.followed.v1";
    public static final String AUTHOR_UNFOLLOWED = "author.unfollowed.v1";
    public static final String PLAYLIST_CREATED = "playlist.created.v1";
    public static final String PLAYLIST_UPDATED = "playlist.updated.v1";
    public static final String PLAYLIST_DELETED = "playlist.deleted.v1";

    private RecommendationEventTypes() {
    }
}
