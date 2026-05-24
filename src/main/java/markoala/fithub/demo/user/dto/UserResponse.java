package markoala.fithub.demo.user.dto;

import markoala.fithub.demo.user.JobRole;
import markoala.fithub.demo.user.User;

public record UserResponse(
        Long id,
        String username,
        String nickname,
        String email,
        JobRole jobRole
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getJobRole()
        );
    }
}
