package markoala.fithub.demo.application.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 회의-스텝 승인 요청 DTO
 */
public record MeetingStepConfirmationRequest(
    @JsonProperty("planner_confirm_yn") String plannerConfirmYn,   // "Approved" 등
    @JsonProperty("developer_confirm_yn") String developerConfirmYn // "Approved" 등
) {}
