package markoala.fithub.demo.domain.project.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "프로젝트 참여 사용자 정보")
public record ProjectDetailMemberResponse(
        @JsonProperty("user_id")
        @Schema(description = "사용자 ID", example = "1")
        Long userId,

        @Schema(description = "사용자 닉네임", example = "planner")
        String nickname
) {
}
