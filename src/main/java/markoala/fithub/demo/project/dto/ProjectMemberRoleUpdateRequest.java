package markoala.fithub.demo.project.dto;

import jakarta.validation.constraints.NotBlank;

public record ProjectMemberRoleUpdateRequest(
        @NotBlank String role
) {}
