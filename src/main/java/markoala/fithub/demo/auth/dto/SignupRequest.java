package markoala.fithub.demo.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import markoala.fithub.demo.user.JobRole;

public record SignupRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        @NotNull(message = "직군(jobRole)은 필수입니다.")
        JobRole jobRole
) {}
