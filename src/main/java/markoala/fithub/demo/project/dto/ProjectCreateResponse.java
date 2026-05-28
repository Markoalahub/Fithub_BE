package markoala.fithub.demo.project.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ProjectCreateResponse(
        @JsonProperty("project_id") Long projectId,
        @JsonProperty("project_name") String projectName
) {
}
