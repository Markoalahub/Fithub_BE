package markoala.fithub.demo.issue.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Issue 상태 업데이트 요청
 *
 * @param status 새로운 상태 (OPEN, CLOSED) (필수)
 * @param repoUrl GitHub 저장소 URL (필수)
 */
public record UpdateIssueStatusRequest(
        @NotBlank(message = "status는 필수입니다")
        @Schema(description = "새로운 상태 (OPEN, CLOSED)", example = "CLOSED")
        String status,

        @NotBlank(message = "repoUrl은 필수입니다")
        @Schema(description = "GitHub 저장소 URL", example = "https://github.com/owner/repo")
        String repoUrl
) {}
