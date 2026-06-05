package markoala.fithub.demo.domain.project.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ProjectDetailResponse(
        @JsonProperty("project_id")
        Long projectId,

        @JsonProperty("project_name")
        String projectName,

        @JsonProperty("project_description")
        String projectDescription,

        List<ProjectDetailMemberResponse> members,

        @JsonProperty("member_count")
        int memberCount
) {
}
