package markoala.fithub.demo.meeting.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

import java.util.Map;

public record MeetingLogCreateRequest(
        @JsonProperty("project_id")
        Long projectId,
        String content,
        @JsonProperty("attendee_user_ids")
        List<Long> attendeeUserIds,
        @JsonProperty("conversation_type")
        String conversationType,
        @JsonProperty("translation_history")
        Map<String, Object> translationHistory
) {}
