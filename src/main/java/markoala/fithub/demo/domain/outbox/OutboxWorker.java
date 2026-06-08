package markoala.fithub.demo.domain.outbox;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OutboxWorker {

    private static final Logger log = LoggerFactory.getLogger(OutboxWorker.class);

    private final OutboxEventRepository outboxEventRepository;
    private final AiCleanupClient aiCleanupClient;

    @Value("${outbox.worker.batch-size:20}")
    private int batchSize;

    @Value("${outbox.worker.max-retries:5}")
    private int maxRetries;

    @Scheduled(fixedDelayString = "${outbox.worker.fixed-delay-ms:5000}")
    @Transactional
    public void processPendingEvents() {
        List<OutboxEvent> events = outboxEventRepository.findDueEvents(
                OutboxEventStatus.PENDING,
                LocalDateTime.now(),
                PageRequest.of(0, batchSize)
        );

        for (OutboxEvent event : events) {
            processEvent(event);
        }
    }

    private void processEvent(OutboxEvent event) {
        event.markProcessing();
        try {
            if (event.getEventType() != OutboxEventType.PROJECT_DELETED) {
                throw new IllegalStateException("Unsupported outbox event type: " + event.getEventType());
            }
            aiCleanupClient.deleteProjectResources(event.getAggregateId());
            event.markDone();
            log.info("[OutboxWorker] Processed {} for aggregateId={}", event.getEventType(), event.getAggregateId());
        } catch (Exception e) {
            long delaySeconds = retryDelaySeconds(event.getRetryCount() + 1);
            event.markRetry(e, maxRetries, delaySeconds);
            log.warn(
                    "[OutboxWorker] Failed {} for aggregateId={}, retryCount={}, status={}",
                    event.getEventType(),
                    event.getAggregateId(),
                    event.getRetryCount(),
                    event.getStatus(),
                    e
            );
        }
    }

    private long retryDelaySeconds(int nextAttempt) {
        return Math.min(60L * nextAttempt, 3_600L);
    }
}
