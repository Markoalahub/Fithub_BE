package markoala.fithub.demo.domain.project.dto;

import jakarta.validation.constraints.NotBlank;

public record ProjectMemberRoleUpdateRequest(
        @NotBlank String role
) {}
