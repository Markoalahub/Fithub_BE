package markoala.fithub.demo.domain.project.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "프로젝트 상세 조회 응답")
public record ProjectDetailResponse(
        @JsonProperty("project_id")
        @Schema(description = "프로젝트 ID", example = "1")
        Long projectId,

        @JsonProperty("project_name")
        @Schema(description = "프로젝트명", example = "Fithub")
        String projectName,

        @JsonProperty("project_description")
        @Schema(description = "프로젝트 내용", example = "AI 기반 피트니스 협업 프로젝트")
        String projectDescription,

        @Schema(description = "프로젝트에 참여 중인 사용자 목록")
        List<ProjectDetailMemberResponse> members,

        @JsonProperty("member_count")
        @Schema(description = "프로젝트 팀원 총 인원 수", example = "2")
        int memberCount
) {
}
