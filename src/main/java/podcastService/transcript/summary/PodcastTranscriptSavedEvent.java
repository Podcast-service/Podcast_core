package podcastService.transcript.summary;

import java.util.UUID;

public record PodcastTranscriptSavedEvent(
        UUID podcastId,
        String language,
        String source
) {
}
