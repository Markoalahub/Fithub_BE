package markoala.fithub.demo.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import markoala.fithub.demo.domain.user.JobRole;
import markoala.fithub.demo.domain.user.User;

public record UserMeResponse(
        @JsonProperty("user_id")
        Long userId,

        String nickname,

        @JsonProperty("job_role")
        JobRole jobRole
) {
    public static UserMeResponse from(User user) {
        return new UserMeResponse(
                user.getId(),
                user.getNickname(),
                user.getJobRole()
        );
    }
}
