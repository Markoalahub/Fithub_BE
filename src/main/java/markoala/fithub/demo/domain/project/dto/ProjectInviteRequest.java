package markoala.fithub.demo.domain.project.dto;

import jakarta.validation.constraints.NotBlank;

public record ProjectInviteRequest(
        @NotBlank String nickname // 초대하려고 하는 사람의 닉네임
) {}
