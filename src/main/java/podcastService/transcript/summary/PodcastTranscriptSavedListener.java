package podcastService.transcript.summary;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PodcastTranscriptSavedListener {

    private final SummaryGenerationService summaryGenerationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPodcastTranscriptSaved(PodcastTranscriptSavedEvent event) {
        try {
            log.info("Podcast transcript saved event received after commit, podcastId={}, language={}, source={}",
                    event.podcastId(), event.language(), event.source());
            summaryGenerationService.generateIfMissing(event.podcastId(), event.language());
        } catch (Exception exception) {
            log.warn("Podcast summary auto-generation failed, podcastId={}, language={}, reason={}",
                    event.podcastId(), event.language(), safeReason(exception));
        }
    }

    private String safeReason(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
