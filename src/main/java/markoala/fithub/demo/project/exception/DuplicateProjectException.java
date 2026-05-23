package markoala.fithub.demo.project.exception;

public class DuplicateProjectException extends RuntimeException {
    public DuplicateProjectException(String message) {
        super(message);
    }
}
