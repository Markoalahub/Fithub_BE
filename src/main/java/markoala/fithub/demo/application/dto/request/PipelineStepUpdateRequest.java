package markoala.fithub.demo.application.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Optional;

public record PipelineStepUpdateRequest(
        @JsonProperty("step_task_description")
        Optional<String> stepTaskDescription,
        @JsonProperty("step_details")
        Optional<List<String>> stepDetails,
        @JsonProperty("step_sequence_number")
        Optional<Integer> stepSequenceNumber,
        @JsonProperty("step_github_status")
        Optional<String> stepGithubStatus,
        @JsonProperty("step_planner_confirm_yn")
        Optional<String> stepPlannerConfirmYn,
        @JsonProperty("step_developer_confirm_yn")
        Optional<String> stepDeveloperConfirmYn,
        Optional<String> duration,
        @JsonProperty("tech_stack")
        Optional<String> techStack,
        Optional<String> origin
) {}
