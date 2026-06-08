package markoala.fithub.demo.domain.issue.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GithubRepositoryCreateRequest(
        @NotBlank String repoUrl,
        @NotBlank String repoType,
        String category
) {}
