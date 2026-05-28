package markoala.fithub.demo.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

/**
 * 회의-스텝 승인 관계 조회 응답 DTO
 */
public record MeetingStepRelationResponse(
    Long id,
    @JsonProperty("meeting_log_id") Long meetingLogId,
    @JsonProperty("pipeline_step_id") Long pipelineStepId,
    @JsonProperty("planner_confirm_yn") String plannerConfirmYn,
    @JsonProperty("developer_confirm_yn") String developerConfirmYn,
    @JsonProperty("confirmed_at") LocalDateTime confirmedAt
) {}
