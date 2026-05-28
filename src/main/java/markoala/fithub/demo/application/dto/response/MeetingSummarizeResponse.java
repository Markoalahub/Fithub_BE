package markoala.fithub.demo.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record MeetingSummarizeResponse(
        @JsonProperty("meeting_log_id")
        Long meetingLogId,
        String summary,
        @JsonProperty("derived_steps")
        List<String> derivedSteps
) {}
