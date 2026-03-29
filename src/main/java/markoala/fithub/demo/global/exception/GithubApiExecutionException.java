package markoala.fithub.demo.global.exception;

import org.springframework.http.HttpStatus;

/**
 * GitHub API 호출 중 발생한 예외를 도메인 특화 예외로 래핑합니다.
 * IOException 등의 저수준 예외를 비즈니스 레이어에서 적절히 처리할 수 있도록 합니다.
 */
public class GithubApiExecutionException extends RuntimeException {

    private final HttpStatus httpStatus;

    public GithubApiExecutionException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    public GithubApiExecutionException(String message, Throwable cause, HttpStatus httpStatus) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

    public GithubApiExecutionException(String message) {
        super(message);
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
