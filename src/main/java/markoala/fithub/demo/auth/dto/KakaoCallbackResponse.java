package markoala.fithub.demo.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record KakaoCallbackResponse(
        boolean success,
        String kakaoAccessToken,
        String accessToken,
        String refreshToken,
        UserDto user
) {
    public record UserDto(Long id) {}
}
