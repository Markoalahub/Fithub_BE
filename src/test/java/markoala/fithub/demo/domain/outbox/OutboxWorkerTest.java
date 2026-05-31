package markoala.fithub.demo.domain.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OutboxWorkerTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private AiCleanupClient aiCleanupClient;

    @InjectMocks
    private OutboxWorker outboxWorker;

    @Test
    @DisplayName("PROJECT_DELETED 이벤트 처리 성공 시 DONE 상태로 변경")
    void processProjectDeletedEventSuccess() {
        OutboxEvent event = OutboxEvent.projectDeleted(1L);
        ReflectionTestUtils.setField(outboxWorker, "batchSize", 20);
        ReflectionTestUtils.setField(outboxWorker, "maxRetries", 5);

        when(outboxEventRepository.findDueEvents(
                eq(OutboxEventStatus.PENDING),
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(List.of(event));

        outboxWorker.processPendingEvents();

        verify(aiCleanupClient).deleteProjectResources(1L);
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.DONE);
        assertThat(event.getProcessedAt()).isNotNull();
    }

    @Test
    @DisplayName("PROJECT_DELETED 이벤트 처리 실패 시 재시도 예약")
    void processProjectDeletedEventFailureSchedulesRetry() {
        OutboxEvent event = OutboxEvent.projectDeleted(1L);
        ReflectionTestUtils.setField(outboxWorker, "batchSize", 20);
        ReflectionTestUtils.setField(outboxWorker, "maxRetries", 5);

        when(outboxEventRepository.findDueEvents(
                eq(OutboxEventStatus.PENDING),
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(List.of(event));
        doThrow(new RuntimeException("AI server unavailable"))
                .when(aiCleanupClient).deleteProjectResources(1L);

        outboxWorker.processPendingEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(event.getRetryCount()).isEqualTo(1);
        assertThat(event.getErrorMessage()).contains("AI server unavailable");
        assertThat(event.getNextRetryAt()).isAfter(LocalDateTime.now());
    }
}
