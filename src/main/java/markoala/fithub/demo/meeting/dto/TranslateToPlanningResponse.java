package markoala.fithub.demo.meeting.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record TranslateToPlanningResponse(
    @JsonProperty("meeting_id") Long meetingId,
    @JsonProperty("original_statement") String originalStatement,
    @JsonProperty("ai_translation") PlanningTranslationResult aiTranslation,
    @JsonProperty("saved_at") LocalDateTime savedAt
) {}
