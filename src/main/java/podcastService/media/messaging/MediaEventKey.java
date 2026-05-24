package podcastService.media.messaging;

public record MediaEventKey(
        String type,
        String event
) {
    public MediaEventKey {
        type = normalize(type);
        event = normalize(event);
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }
}
