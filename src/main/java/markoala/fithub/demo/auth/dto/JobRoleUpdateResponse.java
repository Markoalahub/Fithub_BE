package markoala.fithub.demo.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record JobRoleUpdateResponse(
        boolean success,
        String message,
        UserDto user
) {
    public record UserDto(Long id, String username, String email, String jobRole) {}

    public static JobRoleUpdateResponse success(String message, Long id, String username, String email, String jobRole) {
        return new JobRoleUpdateResponse(true, message, new UserDto(id, username, email, jobRole));
    }

    public static JobRoleUpdateResponse fail(String message) {
        return new JobRoleUpdateResponse(false, message, null);
    }
}
