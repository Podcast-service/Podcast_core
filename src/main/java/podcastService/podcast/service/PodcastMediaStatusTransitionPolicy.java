package podcastService.podcast.service;

import org.springframework.stereotype.Component;
import podcastService.podcast.entity.Status;

import java.util.EnumMap;
import java.util.Map;

@Component
public class PodcastMediaStatusTransitionPolicy {

    private static final Map<Status, Integer> MEDIA_ORDER = new EnumMap<>(Status.class);

    static {
        MEDIA_ORDER.put(Status.DRAFT, 0);
        MEDIA_ORDER.put(Status.UPLOADING, 1);
        MEDIA_ORDER.put(Status.UPLOADED, 2);
        MEDIA_ORDER.put(Status.PROCESSING, 3);
        MEDIA_ORDER.put(Status.PROCESSED, 4);
        MEDIA_ORDER.put(Status.FAILED, 5);
        MEDIA_ORDER.put(Status.PUBLISHED, 6);
        MEDIA_ORDER.put(Status.ARCHIVED, 7);
    }

    public boolean canMoveTo(Status current, Status target) {
        if (current == null || target == null) {
            return false;
        }
        if (current == Status.PUBLISHED || current == Status.ARCHIVED) {
            return current == target;
        }
        if (target == Status.FAILED) {
            return true;
        }
        return rank(target) >= rank(current);
    }

    public boolean allowsPublication(Status status) {
        return status == Status.PROCESSED;
    }

    private int rank(Status status) {
        return MEDIA_ORDER.getOrDefault(status, -1);
    }
}
