package markoala.fithub.demo.pipeline.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateIssueFromStepRequest(
        @NotNull(message = "Repository ID is required")
        Long repositoryId,

        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "Description is required")
        String description,

        @NotBlank(message = "Repository URL is required")
        String repoUrl
) {}
