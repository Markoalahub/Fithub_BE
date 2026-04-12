package markoala.fithub.demo.pipeline.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PipelineGenerateAllRequest(
        @NotNull Long projectId,
        @NotBlank String requirements
) {}
