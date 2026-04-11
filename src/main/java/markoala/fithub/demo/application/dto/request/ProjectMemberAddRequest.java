package markoala.fithub.demo.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProjectMemberAddRequest(
        @NotNull Long userId,
        @NotBlank String role
) {}
