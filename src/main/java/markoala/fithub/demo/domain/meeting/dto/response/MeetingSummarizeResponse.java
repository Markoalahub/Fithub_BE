package markoala.fithub.demo.domain.meeting.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record MeetingSummarizeResponse(
        @JsonProperty("meeting_log_id")
        Long meetingLogId,
        String summary,
        @JsonProperty("derived_steps")
        List<String> derivedSteps
) {}
