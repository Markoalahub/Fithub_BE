package markoala.fithub.demo.domain.project.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ProjectDetailMemberResponse(
        @JsonProperty("user_id")
        Long userId,

        String nickname
) {
}
