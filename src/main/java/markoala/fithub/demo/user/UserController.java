package markoala.fithub.demo.user;

import lombok.RequiredArgsConstructor;
import markoala.fithub.demo.user.dto.OnboardingRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;

    @PostMapping("/onboarding")
    public ResponseEntity<?> onboardUser(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid OnboardingRequest request
    ) {
        userService.completeOnboarding(userId, request.nickname(), request.jobRole());
        return ResponseEntity.ok(Map.of("success", true, "message", "온보딩이 완료되었습니다."));
    }

}
