package markoala.fithub.demo.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Ouroboros 인터뷰 응답 DTO
 */
public record InterviewResponse(
    @JsonProperty("ai_reply")
    String aiReply,

    @JsonProperty("options")
    List<String> options,
    
    @JsonProperty("ambiguity_score")
    int ambiguityScore,
    
    @JsonProperty("is_ready")
    boolean isReady
) {}
