package markoala.fithub.demo.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import markoala.fithub.demo.domain.user.JobRole;
import markoala.fithub.demo.domain.user.User;

public record UserMeResponse(
        @JsonProperty("user_id")
        Long userId,

        String nickname,

        @JsonProperty("job_role")
        JobRole jobRole,

        @JsonProperty("ai_pipeline_generation_remaining_count")
        int aiPipelineGenerationRemainingCount
) {
    public static UserMeResponse from(User user) {
        return new UserMeResponse(
                user.getId(),
                user.getNickname(),
                user.getJobRole(),
                user.getAiPipelineGenerationRemainingCount()
        );
    }
}
