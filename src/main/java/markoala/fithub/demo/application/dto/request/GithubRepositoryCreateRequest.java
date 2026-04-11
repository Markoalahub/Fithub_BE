package markoala.fithub.demo.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GithubRepositoryCreateRequest(
        @NotBlank String repoUrl,
        @NotBlank String repoType,
        String category
) {}
