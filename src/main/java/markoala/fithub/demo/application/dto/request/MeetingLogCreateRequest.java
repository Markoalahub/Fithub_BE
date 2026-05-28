package markoala.fithub.demo.application.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record MeetingLogCreateRequest(
        @JsonProperty("project_id")
        Long projectId,
        String content,
        @JsonProperty("attendee_user_ids")
        List<Long> attendeeUserIds
) {}
