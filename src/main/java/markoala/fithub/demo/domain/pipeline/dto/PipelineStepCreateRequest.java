package markoala.fithub.demo.domain.pipeline.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PipelineStepCreateRequest(
        String title,
        String description,
        @JsonProperty("is_completed")
        Boolean isCompleted,
        String origin
) {}
