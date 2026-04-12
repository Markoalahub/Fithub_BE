package markoala.fithub.demo.issue.dto;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Issue를 GitHub로 동기화 요청
 *
 * @param repoUrl GitHub 저장소 URL (필수)
 */
public record SyncIssueToGitHubRequest(
        @NotBlank(message = "repoUrl은 필수입니다")
        @Schema(description = "GitHub 저장소 URL", example = "https://github.com/owner/repo")
        String repoUrl
) {}
