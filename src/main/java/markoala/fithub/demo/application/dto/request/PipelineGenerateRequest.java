package markoala.fithub.demo.application.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record PipelineGenerateRequest(
        @JsonProperty("project_id")
        Long projectId,
        String requirements,
        String category
) {}
