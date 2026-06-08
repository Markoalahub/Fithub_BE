package markoala.fithub.demo.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SignupResponse(
        boolean success,
        String message,
        String accessToken,
        String refreshToken
) {}
