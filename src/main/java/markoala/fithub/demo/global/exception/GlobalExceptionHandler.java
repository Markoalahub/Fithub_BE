package markoala.fithub.demo.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 전역 예외 처리기
 * GithubApiExecutionException을 포착하여 적절한 HTTP 상태 코드와 에러 메시지를 반환합니다.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(GithubApiExecutionException.class)
    public ResponseEntity<Map<String, Object>> handleGithubApiException(GithubApiExecutionException ex) {
        log.error("[GitHub API Error] {}", ex.getMessage(), ex);

        Map<String, Object> errorBody = Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", ex.getHttpStatus().value(),
                "error", ex.getHttpStatus().getReasonPhrase(),
                "message", ex.getMessage()
        );

        return ResponseEntity.status(ex.getHttpStatus()).body(errorBody);
    }
}
