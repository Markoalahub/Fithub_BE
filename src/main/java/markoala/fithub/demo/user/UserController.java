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

    @GetMapping()
    public ResponseEntity<?> checkNicknameDuplicate(@RequestParam String nickname) {
        boolean isDuplicate = userService.isNicknameDuplicate(nickname);
        return ResponseEntity.ok(Map.of(
                "isDuplicate", isDuplicate,
                "message", isDuplicate ? "이미 사용 중인 닉네임입니다." : "사용 가능한 닉네임입니다."
        ));
    }

    @GetMapping("/check-email")
    public ResponseEntity<?> checkEmailDuplicate(@RequestParam String email) {
        boolean isDuplicate = userService.isEmailDuplicate(email);
        return ResponseEntity.ok(Map.of(
                "isDuplicate", isDuplicate,
                "message", isDuplicate ? "이미 사용 중인 이메일입니다." : "사용 가능한 이메일입니다."
        ));
    }
}
