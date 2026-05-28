package markoala.fithub.demo.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DevTokenResponse(
        boolean success,
        String message,
        String accessToken,
        Long userId,
        String username
) {
    public static DevTokenResponse success(String accessToken, Long userId, String username) {
        return new DevTokenResponse(true, null, accessToken, userId, username);
    }

    public static DevTokenResponse fail(String message) {
        return new DevTokenResponse(false, message, null, null, null);
    }
}
