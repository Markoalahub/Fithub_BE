package markoala.fithub.demo.meeting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MeetingCreateRequest(
        @NotNull Long projectId,
        @NotBlank String content,
        @NotNull Long proposerId,
        @NotNull Long recipientId
) {}
