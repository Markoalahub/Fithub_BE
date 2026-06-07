package markoala.fithub.demo.domain.project.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "프로젝트 수정 응답")
public record ProjectUpdateResponse(
        @JsonProperty("project_id")
        @Schema(description = "프로젝트 ID", example = "4")
        Long projectId,

        @JsonProperty("project_name")
        @Schema(description = "프로젝트명", example = "Fithub 리뉴얼")
        String projectName,

        @JsonProperty("project_description")
        @Schema(description = "프로젝트 내용", example = "AI 협업 기능을 강화한 프로젝트")
        String projectDescription,

        @JsonProperty("creator_id")
        @Schema(description = "프로젝트 생성자 ID", example = "1")
        Long creatorId
) {
}
