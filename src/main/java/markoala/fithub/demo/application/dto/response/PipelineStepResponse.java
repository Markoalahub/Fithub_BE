package markoala.fithub.demo.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 수정된 v3 파이프라인 스텝 응답 DTO
 */
public record PipelineStepResponse(
        Long id,
        @JsonProperty("step_task_description")
        String stepTaskDescription,
        @JsonProperty("step_sequence_number")
        Integer stepSequenceNumber,
        String duration,
        @JsonProperty("tech_stack")
        String techStack
) {}
