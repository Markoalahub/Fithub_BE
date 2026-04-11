package markoala.fithub.demo.global.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 전역 예외 처리기
 * GithubApiExecutionException 및 업로드 관련 예외를 포착하여 적절한 HTTP 상태 코드와 에러 메시지를 반환합니다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        log.error("[Upload Error] {}", ex.getMessage(), ex);

        Map<String, Object> errorBody = Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", HttpStatus.PAYLOAD_TOO_LARGE.value(),
                "error", HttpStatus.PAYLOAD_TOO_LARGE.getReasonPhrase(),
                "message", "File size exceeds maximum allowed limit"
        );

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(errorBody);
    }
}
