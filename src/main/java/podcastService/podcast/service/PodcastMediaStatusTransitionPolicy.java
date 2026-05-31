package podcastService.podcast.service;

import org.springframework.stereotype.Component;
import podcastService.podcast.entity.Status;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

@Component
public class PodcastMediaStatusTransitionPolicy {

    private static final Map<Status, Set<Status>> ALLOWED_TRANSITIONS = new EnumMap<>(Status.class);

    static {
        ALLOWED_TRANSITIONS.put(Status.DRAFT, Set.of(Status.DRAFT, Status.UPLOADING, Status.UPLOADED, Status.FAILED));
        ALLOWED_TRANSITIONS.put(Status.UPLOADING, Set.of(Status.UPLOADING, Status.UPLOADED, Status.FAILED));
        ALLOWED_TRANSITIONS.put(Status.UPLOADED, Set.of(Status.UPLOADED, Status.PROCESSING, Status.FAILED));
        ALLOWED_TRANSITIONS.put(Status.PROCESSING, Set.of(Status.PROCESSING, Status.PROCESSED, Status.FAILED));
        ALLOWED_TRANSITIONS.put(Status.PROCESSED, Set.of(Status.PROCESSED));
        ALLOWED_TRANSITIONS.put(Status.FAILED, Set.of(Status.FAILED, Status.UPLOADING));
        ALLOWED_TRANSITIONS.put(Status.PUBLISHED, Set.of(Status.PUBLISHED));
        ALLOWED_TRANSITIONS.put(Status.ARCHIVED, Set.of(Status.ARCHIVED));
    }

    public boolean canMoveTo(Status current, Status target) {
        if (current == null || target == null) {
            return false;
        }
        return ALLOWED_TRANSITIONS.getOrDefault(current, Set.of()).contains(target);
    }

    public boolean allowsPublication(Status status) {
        return status == Status.PROCESSED;
    }

}
