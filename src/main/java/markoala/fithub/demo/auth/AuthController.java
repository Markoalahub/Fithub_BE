package markoala.fithub.demo.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import markoala.fithub.demo.auth.dto.SignupRequest;
import markoala.fithub.demo.global.security.jwt.JwtProvider;
import markoala.fithub.demo.github.service.GithubRepositoryService;
import markoala.fithub.demo.user.User;
import markoala.fithub.demo.user.UserService;
import markoala.fithub.demo.user.JobRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "GitHub OAuth 인증 API")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final GithubRepositoryService githubRepositoryService;
    private final UserService userService;
    private final JwtProvider jwtProvider;
    private final KakaoService kakaoService;

    @Value("${github.client-id}")
    private String githubClientId;

    @Value("${github.client-secret}")
    private String githubClientSecret;

    @Value("${github.redirect-uri}")
    private String githubRedirectUri;

    @Value("${kakao.client-id}")
    private String kakaoClientId;

    @Value("${kakao.redirect-uri}")
    private String kakaoRedirectUri;

    public AuthController(
            GithubRepositoryService githubRepositoryService,
            UserService userService,
            JwtProvider jwtProvider,
            KakaoService kakaoService
    ) {
        this.githubRepositoryService = githubRepositoryService;
        this.userService = userService;
        this.jwtProvider = jwtProvider;
        this.kakaoService = kakaoService;
    }

    @GetMapping("/login")
    @Operation(
            summary = "GitHub OAuth 로그인",
            description = "GitHub OAuth 인증 페이지로 자동 리다이렉트합니다"
    )
    public String login() {
        log.info("[Auth] Redirecting to GitHub OAuth");

        String githubAuthUrl = String.format(
                "https://github.com/login/oauth/authorize?client_id=%s&redirect_uri=%s&scope=repo,user,read:org",
                githubClientId,
                githubRedirectUri
        );

        return "redirect:" + githubAuthUrl;
    }

    @GetMapping("/github/callback")
    @ResponseBody
    @Operation(
            summary = "GitHub OAuth 콜백",
            description = "GitHub에서 리다이렉트되는 콜백 엔드포인트. JWT 토큰, GitHub Access Token을 발급합니다. 신규 유저는 requiresSignup=true"
    )
    public Map<String, Object> githubCallback(
            @Parameter(description = "GitHub OAuth 인증 코드", required = true)
            @RequestParam String code,
            @Parameter(description = "CSRF 방지용 상태 토큰")
            @RequestParam(required = false) String state
    ) throws IOException {
        log.info("[Auth] Processing GitHub callback with code: {}", code);

        // 1. code → GitHub access token 교환
        String githubAccessToken = githubRepositoryService.exchangeCodeForToken(code);
        log.info("[Auth] GitHub access token acquired");

        // 2. GitHub 사용자 정보 조회
        Map<String, Object> userInfo = githubRepositoryService.getUserInfoFromGithub(githubAccessToken);
        String githubLogin = (String) userInfo.get("login");
        String githubEmail = (String) userInfo.get("email");  // 비공개 설정 시 null
        Long githubId = ((Number) userInfo.get("id")).longValue();

        log.info("[Auth] GitHub user info: login={}, email={}", githubLogin, githubEmail);

        // 3. DB에 사용자 저장 또는 업데이트
        User user = userService.findOrCreateGithubUser(githubLogin, githubEmail, githubId, githubAccessToken);
        log.info("[Auth] User saved/updated: id={}, username={}", user.getId(), user.getUsername());

        // 4. JWT 토큰 발급
        String accessToken = jwtProvider.generateAccessToken(user);
        String refreshToken = jwtProvider.generateRefreshToken(user);

        log.info("[Auth] JWT tokens generated for user: {}", user.getId());

        // 5. 응답 구성
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("requiresSignup", !user.isRegistered());  // 신규 유저면 true → 프론트는 회원가입 화면으로 이동
        response.put("accessToken", accessToken);
        response.put("refreshToken", refreshToken);
        response.put("githubAccessToken", githubAccessToken);
        response.put("oauthEmail", githubEmail);  // GitHub 이메일 (null이면 회원가입 화면에서 직접 입력)

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("username", user.getUsername());
        userMap.put("email", user.getEmail() != null ? user.getEmail() : "");
        userMap.put("jobRole", user.getJobRole() != null ? user.getJobRole().name() : "");
        response.put("user", userMap);

        log.info("[Auth] OAuth callback completed. requiresSignup={}, user: {}", !user.isRegistered(), user.getId());

        return response;
    }

    @GetMapping("/kakao/login")
    @Operation(
            summary = "Kakao OAuth 로그인",
            description = "Kakao OAuth 인증 페이지로 자동 리다이렉트합니다"
    )
    public String kakaoLogin() {
        log.info("[Auth] Redirecting to Kakao OAuth");

        String kakaoAuthUrl = String.format(
                "https://kauth.kakao.com/oauth/authorize?client_id=%s&redirect_uri=%s&response_type=code",
                kakaoClientId,
                kakaoRedirectUri
        );

        return "redirect:" + kakaoAuthUrl;
    }

    @GetMapping("/kakao/callback")
    @ResponseBody
    @Operation(
            summary = "Kakao OAuth 콜백",
            description = "Kakao에서 리다이렉트되는 콜백 엔드포인트. JWT 토큰 발급. 신규 유저는 requiresSignup=true, jobRole은 PLANNER 자동 설정"
    )
    @SuppressWarnings("unchecked")
    public Map<String, Object> kakaoCallback(
            @Parameter(description = "Kakao OAuth 인증 코드", required = true)
            @RequestParam String code
    ) throws IOException {
        log.info("[Auth] Processing Kakao callback with code: {}", code);

        // 1. code -> Kakao access token 교환
        String kakaoAccessToken = kakaoService.exchangeCodeForToken(code);
        log.info("[Auth] Kakao access token acquired");

        // 2. Kakao 사용자 정보 조회
        Map<String, Object> userInfo = kakaoService.getUserInfoFromKakao(kakaoAccessToken);
        Long kakaoId = ((Number) userInfo.get("id")).longValue();

        Map<String, Object> properties = (Map<String, Object>) userInfo.get("properties");
        String nickname = properties != null ? (String) properties.get("nickname") : "KakaoUser_" + kakaoId;

        Map<String, Object> kakaoAccount = (Map<String, Object>) userInfo.get("kakao_account");
        String email = kakaoAccount != null ? (String) kakaoAccount.get("email") : null;  // 동의 안 하면 null

        log.info("[Auth] Kakao user info: nickname={}, email={}", nickname, email);

        // 3. DB에 사용자 저장 또는 업데이트
        User user = userService.findOrCreateKakaoUser(nickname, email, kakaoId, kakaoAccessToken);
        log.info("[Auth] User saved/updated: id={}, username={}", user.getId(), user.getUsername());

        // 4. JWT 토큰 발급
        String accessToken = jwtProvider.generateAccessToken(user);
        String refreshToken = jwtProvider.generateRefreshToken(user);

        log.info("[Auth] JWT tokens generated for user: {}", user.getId());

        // 5. 응답 구성 (Kakao 사용자는 PLANNER 역할 자동 설정 예정)
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("requiresSignup", !user.isRegistered());  // 신규 유저면 true
        response.put("accessToken", accessToken);
        response.put("refreshToken", refreshToken);
        response.put("kakaoAccessToken", kakaoAccessToken);
        response.put("oauthEmail", email);  // Kakao 이메일 (null이면 회원가입 화면에서 직접 입력)
        response.put("suggestedJobRole", "PLANNER");  // Kakao 로그인 → 기획자 자동 제안

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("username", user.getUsername());
        userMap.put("email", user.getEmail() != null ? user.getEmail() : "");
        userMap.put("jobRole", user.getJobRole() != null ? user.getJobRole().name() : "");
        response.put("user", userMap);

        log.info("[Auth] Kakao OAuth callback completed. requiresSignup={}, user: {}", !user.isRegistered(), user.getId());

        return response;
    }

    @PostMapping("/signup")
    @ResponseBody
    @Operation(
            summary = "회원가입 완료",
            description = "OAuth 로그인 후 이메일과 직군(jobRole)을 설정하여 회원가입을 완료합니다. " +
                    "Kakao 로그인 사용자는 PLANNER, GitHub 로그인 사용자는 원하는 직군을 선택합니다."
    )
    public ResponseEntity<Map<String, Object>> signup(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody SignupRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        if (userId == null) {
            response.put("success", false);
            response.put("message", "인증 토큰이 필요합니다. OAuth 로그인 후 발급된 accessToken을 사용하세요.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        try {
            User user = userService.completeSignup(userId, request.email(), request.jobRole());
            log.info("[Auth] Signup completed for user: id={}, email={}, jobRole={}", user.getId(), user.getEmail(), user.getJobRole());

            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("username", user.getUsername());
            userMap.put("email", user.getEmail());
            userMap.put("jobRole", user.getJobRole().name());

            response.put("success", true);
            response.put("message", "회원가입이 완료되었습니다.");
            response.put("user", userMap);
            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @GetMapping("/token")
    @ResponseBody
    @Operation(
            summary = "세션에 발급된 JWT 토큰 조회",
            description = "OAuth2 성공 후 세션에 임시 저장된 JWT 토큰을 화면에 JSON으로 반환합니다."
    )
    public Map<String, Object> getSessionToken(jakarta.servlet.http.HttpSession session) {
        String token = (String) session.getAttribute("jwt_token");
        Long userId = (Long) session.getAttribute("user_id");
        String username = (String) session.getAttribute("username");
        String jobRole = (String) session.getAttribute("job_role");

        Map<String, Object> response = new java.util.HashMap<>();
        if (token != null) {
            response.put("success", true);
            response.put("accessToken", token);
            
            Map<String, Object> userMap = new java.util.HashMap<>();
            userMap.put("id", userId != null ? userId : "");
            userMap.put("username", username != null ? username : "");
            userMap.put("jobRole", jobRole != null ? jobRole : "");
            response.put("user", userMap);
        } else {
            response.put("success", false);
            response.put("message", "No token found in session. Please login first.");
        }
        return response;
    }

    /**
     * [개발/테스트 전용] userId로 JWT 토큰 발급
     * 운영 환경에서는 이 엔드포인트를 반드시 제거하세요.
     */
    @GetMapping("/dev/token")
    @ResponseBody
    @Operation(
            summary = "[DEV ONLY] userId로 JWT 토큰 발급",
            description = "개발/테스트 용도로만 사용. userId에 해당하는 사용자의 JWT accessToken을 반환합니다."
    )
    public ResponseEntity<Map<String, Object>> devToken(
            @Parameter(description = "토큰을 발급할 사용자 ID", required = true)
            @RequestParam Long userId
    ) {
        Map<String, Object> response = new HashMap<>();
        return userService.findById(userId)
                .map(user -> {
                    String accessToken = jwtProvider.generateAccessToken(user);
                    response.put("success", true);
                    response.put("accessToken", accessToken);
                    response.put("userId", user.getId());
                    response.put("username", user.getUsername());
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    response.put("success", false);
                    response.put("message", "User not found: " + userId);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
                });
    }

    @PutMapping("/user/job-role")
    @ResponseBody
    @Operation(
            summary = "사용자 직군(JobRole) 설정/수정",
            description = "로그인된 사용자의 직군(PLANNER, FRONTEND, BACKEND, AI)을 설정하거나 수정합니다."
    )
    public Map<String, Object> updateJobRole(
            @Parameter(description = "설정할 직군 (PLANNER, FRONTEND, BACKEND, AI)", required = true)
            @RequestParam JobRole jobRole,
            org.springframework.security.core.Authentication authentication
    ) {
        Map<String, Object> response = new HashMap<>();
        if (authentication == null || !authentication.isAuthenticated()) {
            response.put("success", false);
            response.put("message", "User is not authenticated.");
            return response;
        }

        String username = authentication.getName();
        java.util.Optional<User> userOpt = userService.findByUsername(username);

        if (userOpt.isEmpty()) {
            userOpt = userService.findBySocialLoginId(username);
        }

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            userService.updateJobRole(user.getId(), jobRole);
            response.put("success", true);
            response.put("message", "Job role updated successfully.");
            
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("username", user.getUsername());
            userMap.put("email", user.getEmail() != null ? user.getEmail() : "");
            userMap.put("jobRole", jobRole.name());
            response.put("user", userMap);
        } else {
            response.put("success", false);
            response.put("message", "User not found.");
        }
        return response;
    }
}
