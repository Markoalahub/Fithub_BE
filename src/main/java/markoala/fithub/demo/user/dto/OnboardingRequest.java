package markoala.fithub.demo.user.dto;

import jakarta.validation.constraints.NotBlank;
import markoala.fithub.demo.user.JobRole;

public record OnboardingRequest(
    @NotBlank(message = "닉네임은 필수입니다.")
    String nickname,
    
    JobRole jobRole 
) {}
