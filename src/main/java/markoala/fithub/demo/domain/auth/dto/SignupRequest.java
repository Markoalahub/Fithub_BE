package markoala.fithub.demo.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import markoala.fithub.demo.domain.user.JobRole;

public record SignupRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        @NotBlank(message = "이름(username)은 필수입니다.")
        String username,

        @NotNull(message = "직군(jobRole)은 필수입니다.")
        JobRole jobRole,

        @NotBlank(message = "소셜 고유 ID는 필수입니다.")
        String socialLoginId,

        @NotBlank(message = "소셜 타입은 필수입니다.")
        String socialType, // "GITHUB" 또는 "KAKAO"

        @NotBlank(message = "OAuth Access Token은 필수입니다.")
        String oauthAccessToken
) {}
