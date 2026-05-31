package markoala.fithub.demo.domain.meeting.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MeetingAttendeeCreateRequest(
        @JsonProperty("user_id")
        Long userId
) {}
