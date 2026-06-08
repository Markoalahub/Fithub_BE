package markoala.fithub.demo.domain.meeting.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MeetingAttendeeResponse(
        Long id,
        @JsonProperty("meeting_log_id")
        Long meetingLogId,
        @JsonProperty("user_id")
        Long userId
) {}
