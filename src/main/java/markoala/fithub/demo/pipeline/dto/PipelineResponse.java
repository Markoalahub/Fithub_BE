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
        List<PipelineStepResponse> steps,
        @JsonProperty("user_flow")
        UserFlowResponse userFlow
) {
    // Backward compatible constructor
    public PipelineResponse(Long id, Long projectId, String category, Long version, Boolean isActive, List<PipelineStepResponse> steps) {
        this(id, projectId, category, version, isActive, steps, null);
    }
}
