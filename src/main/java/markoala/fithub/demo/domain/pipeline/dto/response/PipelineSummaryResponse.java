package markoala.fithub.demo.domain.pipeline.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "프로젝트 파이프라인 요약 응답")
public record PipelineSummaryResponse(
        @Schema(description = "파이프라인 ID", example = "33")
        @JsonProperty("pipe_id") Long pipeId,

        @Schema(description = "파이프라인 표시 이름", example = "BE 파이프라인 33")
        @JsonProperty("pipeline_name") String pipelineName,

        @Schema(description = "파이프라인 카테고리", example = "BE")
        String category,

        @Schema(description = "파이프라인에 일대일로 연결된 GitHub repository URL", example = "https://github.com/Markoalahub/Fithub_BE")
        @JsonProperty("github_repo_url") String githubRepoUrl
) {
    public PipelineSummaryResponse(Long pipeId, String pipelineName, String category) {
        this(pipeId, pipelineName, category, null);
    }
}
