package markoala.fithub.demo.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import markoala.fithub.demo.domain.user.JobRole;

public record OnboardingRequest(
    @NotBlank(message = "닉네임은 필수입니다.")
    String nickname,
    
    String jobRole 
) {}
