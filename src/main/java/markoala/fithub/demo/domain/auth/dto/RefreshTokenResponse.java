package markoala.fithub.demo.domain.auth.dto;

public record RefreshTokenResponse(
        String accessToken,
        String refreshToken
) {}
