package markoala.fithub.demo.pipeline.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PipelineStepUpdateRequest(
        @JsonProperty("is_completed")
        Boolean isCompleted
) {}
