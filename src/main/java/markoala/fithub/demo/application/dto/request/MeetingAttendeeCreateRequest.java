package markoala.fithub.demo.application.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MeetingAttendeeCreateRequest(
        @JsonProperty("user_id")
        Long userId
) {}
