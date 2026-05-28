package markoala.fithub.demo.global.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * 전역 예외 처리기
 * ErrorResponse DTO를 통해 일관된 에러 응답을 반환합니다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(GithubApiExecutionException.class)
    public ResponseEntity<ErrorResponse> handleGithubApiException(GithubApiExecutionException ex) {
        log.error("[GitHub API Error] {}", ex.getMessage(), ex);
        return ResponseEntity.status(ex.getHttpStatus())
                .body(ErrorResponse.of(
                        ex.getHttpStatus().value(),
                        ex.getHttpStatus().getReasonPhrase(),
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(markoala.fithub.demo.project.exception.DuplicateProjectException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateProjectException(markoala.fithub.demo.project.exception.DuplicateProjectException ex) {
        log.error("[Duplicate Project Error] {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(
                        HttpStatus.CONFLICT.value(),
                        HttpStatus.CONFLICT.getReasonPhrase(),
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(
                        HttpStatus.CONFLICT.value(),
                        HttpStatus.CONFLICT.getReasonPhrase(),
                        e.getMessage()
                ));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode())
                .body(ErrorResponse.of(
                        e.getStatusCode().value(),
                        ((HttpStatus) e.getStatusCode()).getReasonPhrase(),
                        e.getReason()
                ));
    }
}
