package markoala.fithub.demo.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PipelineStepAddRequest(
        @NotBlank String title,
        String description
) {}
