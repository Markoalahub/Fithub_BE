package markoala.fithub.demo.pipeline.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 파이프라인을 GitHub로 동기화 요청
 *
 * @param repositoryId 저장소 ID (필수)
 * @param accessToken GitHub Access Token (필수)
 */
public record SyncPipelineToGitHubRequest(
        @NotNull(message = "repositoryId는 필수입니다")
        @Schema(description = "저장소 ID", example = "1")
        Long repositoryId,

        @NotBlank(message = "accessToken은 필수입니다")
        @Schema(description = "GitHub Access Token", example = "ghp_xxxxxx")
        String accessToken
) {}
