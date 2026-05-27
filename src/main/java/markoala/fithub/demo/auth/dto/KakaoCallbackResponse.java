package markoala.fithub.demo.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record KakaoCallbackResponse(
        boolean isNew,
        String accessToken,
        String refreshToken
) {}
