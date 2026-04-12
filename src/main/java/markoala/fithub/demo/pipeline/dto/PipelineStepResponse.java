package markoala.fithub.demo.pipeline.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PipelineStepResponse(
        Long id,
        @JsonProperty("pipeline_id")
        Long pipelineId,
        String title,
        String description,
        @JsonProperty("is_completed")
        Boolean isCompleted,
        String origin
) {}
