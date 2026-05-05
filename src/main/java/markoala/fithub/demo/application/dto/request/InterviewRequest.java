package markoala.fithub.demo.application.dto.request;

import java.util.List;
import java.util.Map;

/**
 * Ouroboros 인터뷰 요청 DTO
 */
public record InterviewRequest(
    String user_message,
    List<Map<String, String>> chat_history,
    String context
) {}
