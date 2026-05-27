package markoala.fithub.demo.domain.project.exception;

public class DuplicateProjectException extends RuntimeException {
    public DuplicateProjectException(String message) {
        super(message);
    }
}
