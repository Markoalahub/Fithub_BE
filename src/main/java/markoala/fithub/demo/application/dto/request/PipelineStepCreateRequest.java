package markoala.fithub.demo.application.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PipelineStepCreateRequest(
        @JsonProperty("step_task_description")
        String stepTaskDescription,
        @JsonProperty("step_sequence_number")
        Integer stepSequenceNumber,
        String duration,
        @JsonProperty("tech_stack")
        String techStack,
        String origin
) {}
