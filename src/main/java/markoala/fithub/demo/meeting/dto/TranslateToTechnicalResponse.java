package markoala.fithub.demo.meeting.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record TranslateToTechnicalResponse(
    @JsonProperty("meeting_id") Long meetingId,
    @JsonProperty("original_statement") String originalStatement,
    @JsonProperty("ai_translation") TechnicalTranslationResult aiTranslation,
    @JsonProperty("saved_at") LocalDateTime savedAt
) {}
