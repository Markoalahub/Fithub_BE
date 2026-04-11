package markoala.fithub.demo.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MeetingConfirmStepRequest(
        @NotNull Long pipelineId,
        @NotBlank String title,
        String description
) {}
