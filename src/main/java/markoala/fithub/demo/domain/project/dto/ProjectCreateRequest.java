package markoala.fithub.demo.domain.project.dto;

import jakarta.validation.constraints.NotBlank;

public record ProjectCreateRequest(
        @NotBlank String name,
        String description
) {}
