package markoala.fithub.demo.domain.pipeline.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "파이프라인 GitHub repository URL 연결 응답")
public record PipelineGithubRepositoryResponse(
        @JsonProperty("pipe_id")
        @Schema(description = "파이프라인 ID", example = "43")
        Long pipeId,

        @JsonProperty("project_id")
        @Schema(description = "프로젝트 ID", example = "4")
        Long projectId,

        @Schema(description = "파이프라인 카테고리", example = "BE")
        String category,

        @Schema(description = "파이프라인 버전", example = "2")
        Integer version,

        @JsonProperty("tech_stack")
        @Schema(description = "파이프라인 기술 스택", example = "flutter")
        String techStack,

        @JsonProperty("github_repo_url")
        @Schema(description = "연결된 GitHub repository URL", example = "https://github.com/Markoalahub/tomchaccom")
        String githubRepoUrl
) {
    public static PipelineGithubRepositoryResponse from(PipelineV3Response response) {
        return new PipelineGithubRepositoryResponse(
                response.id(),
                response.projectId(),
                response.category(),
                response.version(),
                response.techStack(),
                response.githubRepoUrl()
        );
    }
}
