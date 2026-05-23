package markoala.fithub.demo.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SessionTokenResponse(
        boolean success,
        String message,
        String accessToken,
        UserDto user
) {
    public record UserDto(Object id, String username, String jobRole) {}

    public static SessionTokenResponse success(String accessToken, Object id, String username, String jobRole) {
        return new SessionTokenResponse(true, null, accessToken, new UserDto(id, username, jobRole));
    }

    public static SessionTokenResponse fail(String message) {
        return new SessionTokenResponse(false, message, null, null);
    }
}
