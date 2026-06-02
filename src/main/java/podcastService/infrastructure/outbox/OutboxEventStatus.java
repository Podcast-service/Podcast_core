package podcastService.infrastructure.outbox;

public enum OutboxEventStatus {
    NEW,
    PROCESSING,
    SENT,
    FAILED
}
