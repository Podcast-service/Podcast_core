package podcastService.infrastructure.messaging.config;

public enum KafkaTopicEvent {
    CREATED("created"),
    UPDATED("updated"),
    DELETED("deleted");

    private final String value;

    KafkaTopicEvent(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
