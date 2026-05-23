package markoala.fithub.demo.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GithubCallbackResponse(
        boolean success,
        String gitAccessToken,
        String accessToken,
        String refreshToken,
        UserDto user
) {
    public record UserDto(Long id) {}
}
