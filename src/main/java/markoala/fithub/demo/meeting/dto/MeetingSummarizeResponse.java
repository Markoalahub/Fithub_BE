package markoala.fithub.demo.meeting.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record MeetingSummarizeResponse(
        @JsonProperty("meeting_log_id")
        Long meetingLogId,
        String summary,
        @JsonProperty("derived_steps")
        List<String> derivedSteps
) {}
