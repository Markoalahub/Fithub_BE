package markoala.fithub.demo.domain.outbox;

public enum OutboxEventStatus {
    PENDING,
    PROCESSING,
    DONE,
    FAILED
}
