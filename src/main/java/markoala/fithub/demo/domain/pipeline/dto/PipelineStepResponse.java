package markoala.fithub.demo.domain.pipeline.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record PipelineStepResponse(
        Long id,
        @JsonProperty("pipeline_id")
        Long pipelineId,
        String title,
        String description,
        @JsonProperty("is_completed")
        Boolean isCompleted,
        String origin,
        @JsonProperty("user_flow_node_id")
        Long userFlowNodeId,
        @JsonProperty("tech_stack")
        List<String> techStack,
        @JsonProperty("depends_on")
        List<Integer> dependsOn,
        Integer priority
) {
    // Backward compatible constructor
    public PipelineStepResponse(Long id, Long pipelineId, String title, String description, Boolean isCompleted, String origin) {
        this(id, pipelineId, title, description, isCompleted, origin, null, null, null, null);
    }
}
