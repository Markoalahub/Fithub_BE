package markoala.fithub.demo.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MeetingStepRelationResponse(
        Long id,
        @JsonProperty("meeting_log_id")
        Long meetingLogId,
        @JsonProperty("pipeline_step_id")
        Long pipelineStepId
) {}
