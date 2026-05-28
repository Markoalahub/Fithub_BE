package markoala.fithub.demo.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SignupResponse(
        boolean success,
        String message,
        String accessToken,
        String refreshToken
) {}
