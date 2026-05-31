package markoala.fithub.demo.domain.project.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ProjectCreateResponse(
        @JsonProperty("project_id") Long projectId,
        @JsonProperty("project_name") String projectName,
        @JsonProperty("creator_id") Long creatorId,
        @JsonProperty("creator_nickname") String creatorNickname
) {
}
