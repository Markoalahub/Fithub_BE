package markoala.fithub.demo.domain.pipeline.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 최종 v3 파이프라인 생성 응답 DTO
 */
@Schema(description = "V3 파이프라인 응답")
public record PipelineV3Response(
    @JsonProperty("pipe_id")
    @JsonAlias("id")
    @Schema(description = "파이프라인 ID", example = "33")
    Long id,

    @JsonProperty("project_id")
    @Schema(description = "프로젝트 ID", example = "1")
    Long projectId,

    @Schema(description = "파이프라인 카테고리", example = "BE")
    String category,    // "BE", "FE" 등

    @Schema(description = "파이프라인 버전", example = "1")
    Integer version,

    @JsonProperty("tech_stack")
    @Schema(description = "파이프라인 전체 기술 스택", example = "Spring Boot, JPA")
    String techStack, // 파이프라인 전체 기술 스택

    @JsonProperty("github_repo_url")
    @Schema(description = "파이프라인에 일대일로 연결된 GitHub repository URL", example = "https://github.com/Markoalahub/Fithub_BE")
    String githubRepoUrl,

    @Schema(description = "파이프라인 작업 목록")
    @JsonAlias({"feats", "steps"}) List<FeatResponse> feats
) {
    public PipelineV3Response(
            Long id,
            Long projectId,
            String category,
            Integer version,
            String techStack,
            List<FeatResponse> feats
    ) {
        this(id, projectId, category, version, techStack, null, feats);
    }
}
