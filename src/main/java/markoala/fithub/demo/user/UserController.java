package markoala.fithub.demo.user;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Users", description = "사용자 온보딩, 닉네임 중복 체크, 사용자 조회 API")
public class UserController {
    
    private final UserService userService;

    @PostMapping("/onboarding")
    @Operation(summary = "기획자/개발자 온보딩", description = "로그인 후 닉네임과 직군(jobRole)을 설정하여 온보딩을 완료합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "온보딩 완료"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터")
    })
    public ResponseEntity<?> onboardUser(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid OnboardingRequest request
    ) {
        userService.completeOnboarding(userId, request.nickname(), request.jobRole());
        return ResponseEntity.ok(Map.of("success", true, "message", "온보딩이 완료되었습니다."));
    }

    @GetMapping("/check-nickname")
    @Operation(summary = "닉네임 중복 체크", description = "입력한 닉네임이 이미 사용 중인지 확인합니다.")
    @ApiResponse(responseCode = "200", description = "중복 여부 반환")
    public ResponseEntity<?> checkNicknameDuplicate(
            @Parameter(description = "확인할 닉네임", required = true)
            @RequestParam String nickname
    ) {
        boolean isDuplicate = userService.isNicknameDuplicate(nickname);
        return ResponseEntity.ok(Map.of(
                "isDuplicate", isDuplicate,
                "message", isDuplicate ? "이미 사용 중인 닉네임입니다." : "사용 가능한 닉네임입니다."
        ));
    }

    @Hidden
    @GetMapping("/check-email")
    public ResponseEntity<?> checkEmailDuplicate(@RequestParam String email) {
        boolean isDuplicate = userService.isEmailDuplicate(email);
        return ResponseEntity.ok(Map.of(
                "isDuplicate", isDuplicate,
                "message", isDuplicate ? "이미 사용 중인 이메일입니다." : "사용 가능한 이메일입니다."
        ));
    }

    @GetMapping()
    @Operation(summary = "닉네임으로 사용자 조회", description = "닉네임을 기반으로 사용자를 검색합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "사용자 조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    public ResponseEntity<markoala.fithub.demo.user.dto.UserResponse> getUserByNickname(
            @Parameter(description = "조회할 닉네임", required = true)
            @RequestParam String nickname
    ) {
        User user = userService.findByNickname(nickname)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다: " + nickname));
        return ResponseEntity.ok(markoala.fithub.demo.user.dto.UserResponse.from(user));
    }
}

