package markoala.fithub.demo.meeting.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MeetingAttendeeCreateRequest(
        @JsonProperty("user_id")
        Long userId
) {}
