package markoala.fithub.demo.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

public record MeetingLogResponse(
        Long id,
        @JsonProperty("project_id")
        Long projectId,
        String content,
        String summary,
        @JsonProperty("vector_id")
        String vectorId,
        @JsonProperty("created_at")
        LocalDateTime createdAt,
        List<MeetingAttendeeResponse> attendees,
        @JsonProperty("step_relations")
        List<MeetingStepRelationResponse> stepRelations
) {}
