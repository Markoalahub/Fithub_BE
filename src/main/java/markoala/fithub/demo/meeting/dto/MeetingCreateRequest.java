package markoala.fithub.demo.meeting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record MeetingCreateRequest(
        @NotNull Long projectId,
        @NotBlank String content,
        Long proposerId,
        Long recipientId,
        String conversationType,
        Map<String, Object> translationHistory
) {}
