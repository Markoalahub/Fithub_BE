package markoala.fithub.demo.pipeline.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record PipelineResponse(
        Long id,
        @JsonProperty("project_id")
        Long projectId,
        String category,
        Long version,
        @JsonProperty("is_active")
        Boolean isActive,
        List<PipelineStepResponse> steps
) {}
