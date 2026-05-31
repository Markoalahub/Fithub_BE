package markoala.fithub.demo.domain.meeting.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MeetingAttendeeCreateRequest(
        @JsonProperty("user_id")
        Long userId
) {}
