package markoala.fithub.demo.domain.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProjectMemberAddRequest(
        @NotNull Long userId,
        @NotBlank String role
) {}
