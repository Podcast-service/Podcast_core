package podcastService.infrastructure.messaging.config;

public enum KafkaTopicDomain {
    USER("user"),
    PODCAST("podcast");

    private final String value;

    KafkaTopicDomain(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
