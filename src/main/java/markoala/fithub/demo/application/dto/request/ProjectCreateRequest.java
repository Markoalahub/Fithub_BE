package markoala.fithub.demo.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ProjectCreateRequest(
        @NotBlank String name,
        String description
) {}
