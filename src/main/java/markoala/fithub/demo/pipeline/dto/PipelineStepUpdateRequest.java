package markoala.fithub.demo.pipeline.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;

public record PipelineStepUpdateRequest(
        @JsonProperty("title")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        String title,

        @JsonProperty("description")
        String description,

        @JsonProperty("is_completed")
        Boolean isCompleted
) {}
