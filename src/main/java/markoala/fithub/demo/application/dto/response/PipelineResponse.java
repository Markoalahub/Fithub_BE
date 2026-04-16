package markoala.fithub.demo.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record PipelineResponse(
        Long id,
        @JsonProperty("project_id")
        Long projectId,
        String category,
        Long version,
        @JsonProperty("is_active")
        String isActive,
        List<PipelineStepResponse> steps
) {}
