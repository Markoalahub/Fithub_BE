package markoala.fithub.demo.domain.pipeline.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "파이프라인 GitHub repository URL 연결 요청")
public record PipelineGithubRepositoryUpdateRequest(
        @JsonProperty("github_repo_url")
        @NotBlank(message = "GitHub repository URL은 필수입니다.")
        @Size(max = 500, message = "GitHub repository URL은 500자를 초과할 수 없습니다.")
        @Pattern(regexp = "^https?://.+", message = "GitHub repository URL은 http:// 또는 https://로 시작해야 합니다.")
        @Schema(description = "파이프라인과 일대일로 연결할 GitHub repository URL", example = "https://github.com/Markoalahub/Fithub_BE")
        String githubRepoUrl
) {
    public PipelineGithubRepositoryUpdateRequest {
        if (githubRepoUrl != null) {
            githubRepoUrl = githubRepoUrl.trim();
        }
    }
}
