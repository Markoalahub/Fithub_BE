package markoala.fithub.demo.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record PipelineStepResponse(
        Long id,
        @JsonProperty("pipeline_id")
        Long pipelineId,
        @JsonProperty("step_task_description")
        String stepTaskDescription,
        @JsonProperty("step_sequence_number")
        Integer stepSequenceNumber,
        @JsonProperty("step_github_status")
        String stepGithubStatus,
        @JsonProperty("step_planner_confirm_yn")
        String stepPlannerConfirmYn,
        @JsonProperty("step_developer_confirm_yn")
        String stepDeveloperConfirmYn,
        @JsonProperty("step_confirmation_date")
        LocalDateTime stepConfirmationDate,
        @JsonProperty("step_final_confirmed_status")
        String stepFinalConfirmedStatus,
        String duration,
        @JsonProperty("tech_stack")
        String techStack,
        String origin,
        @JsonProperty("created_at")
        LocalDateTime createdAt,
        @JsonProperty("updated_at")
        LocalDateTime updatedAt
) {}
