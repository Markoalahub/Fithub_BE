package markoala.fithub.demo.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ProjectMemberRoleUpdateRequest(
        @NotBlank String role
) {}
