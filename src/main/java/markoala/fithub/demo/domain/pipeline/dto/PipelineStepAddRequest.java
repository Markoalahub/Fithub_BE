package markoala.fithub.demo.domain.pipeline.dto;

import jakarta.validation.constraints.NotBlank;

public record PipelineStepAddRequest(
        @NotBlank String title,
        String description
) {}
