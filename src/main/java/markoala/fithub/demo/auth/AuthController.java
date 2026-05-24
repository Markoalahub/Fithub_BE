package markoala.fithub.demo.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import markoala.fithub.demo.auth.dto.SignupRequest;
import markoala.fithub.demo.auth.dto.GithubCallbackResponse;
import markoala.fithub.demo.auth.dto.KakaoCallbackResponse;
import markoala.fithub.demo.auth.dto.SessionTokenResponse;
import markoala.fithub.demo.auth.dto.SignupResponse;
import markoala.fithub.demo.auth.dto.JobRoleUpdateResponse;
import markoala.fithub.demo.auth.dto.DevTokenResponse;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    @Value("${app.frontend.oauth-callback-url:http://localhost:3000/auth/oauth/callback}")
    private String frontendOAuthCallbackUrl;

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
    public String login(
            @Parameter(description = "로그인 성공 후 프론트 리다이렉트 URL")
            @RequestParam(required = false) String frontendRedirect,
            @Parameter(description = "역할 정보 (pm/dev-fe/dev-be)")
            @RequestParam(required = false) String role
    ) {
        log.info("[Auth] Redirecting to GitHub OAuth");

        StringBuilder githubAuthUrl = new StringBuilder(String.format(
                "https://github.com/login/oauth/authorize?client_id=%s&redirect_uri=%s&scope=repo,user,read:org",
                githubClientId,
                githubRedirectUri
        ));

        String state = encodeState(frontendRedirect, role);
        if (state != null) {
            githubAuthUrl.append("&state=")
                    .append(URLEncoder.encode(state, StandardCharsets.UTF_8));
        }

        return "redirect:" + githubAuthUrl;
    }

    @GetMapping("/github/callback")
    @Operation(
            summary = "GitHub OAuth 콜백",
            description = "GitHub에서 리다이렉트되는 콜백 엔드포인트. 이미 가입된 유저라면 JWT를 즉시 발급하고, 신규 가입 유저라면 가입에 필요한 소셜 정보를 반환합니다."
    )
    public ResponseEntity<?> githubCallback(
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

        // 3. 사용자 존재 여부 및 기존 토큰 확인하여 신규/기존 사용자 구분
        java.util.Optional<User> existingUser = userService.findBySocialLoginId(String.valueOf(githubId));
        boolean isNew = existingUser.isEmpty() || existingUser.get().getGithubAccessToken() == null;

        // 4. GitHub 사용자 정보 기반 조회 또는 생성 완결 (즉시 가입 처리)
        User user = userService.findOrCreateGithubUser(githubLogin, githubEmail, githubId, githubAccessToken);


        // 5. 우리 서비스 전용 JWT 즉시 발급 및 응답 구성
        String accessToken = jwtProvider.generateAccessToken(user);
        String refreshToken = jwtProvider.generateRefreshToken(user);

        log.info("[Auth] GitHub user authenticated and logged in. User: {}, isNew: {}", user.getId(), isNew);

        GithubCallbackResponse response = new GithubCallbackResponse(
                true,
                isNew,
                githubAccessToken,
                accessToken,
                refreshToken,
                new GithubCallbackResponse.UserDto(user.getId())
        );

        Map<String, String> stateMap = decodeState(state);
        String frontendRedirectFromState = stateMap.get("frontendRedirect");
        if (frontendRedirectFromState != null && !frontendRedirectFromState.trim().isBlank()) {
            String frontendRedirect = frontendRedirectFromState.trim();
            String role = stateMap.getOrDefault("role", "dev-fe").trim();
            if (role.isBlank()) {
                role = "dev-fe";
            }

            String redirectUrl = UriComponentsBuilder.fromUriString(frontendRedirect)
                    .queryParam("success", "true")
                    .queryParam("provider", "github")
                    .queryParam("gitAccessToken", response.gitAccessToken())
                    .queryParam("githubAccessToken", response.gitAccessToken())
                    .queryParam("accessToken", response.accessToken())
                    .queryParam("refreshToken", response.refreshToken())
                    .queryParam("userId", response.user().id())
                    .queryParam("username", user.getUsername() != null ? user.getUsername() : "")
                    .queryParam("email", user.getEmail() != null ? user.getEmail() : "")
                    .queryParam("role", role)
                    .queryParam("isNew", isNew)
                    .build()
                    .encode()
                    .toUriString();

            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(redirectUrl))
                    .build();
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/kakao/login")
    @Operation(
            summary = "Kakao OAuth 로그인",
            description = "Kakao OAuth 인증 페이지로 자동 리다이렉트합니다"
    )
    public String kakaoLogin(
            @Parameter(description = "로그인 성공 후 프론트 리다이렉트 URL")
            @RequestParam(required = false) String frontendRedirect,
            @Parameter(description = "역할 정보 (pm/dev-fe/dev-be)")
            @RequestParam(required = false) String role
    ) {
        log.info("[Auth] Redirecting to Kakao OAuth");

        StringBuilder kakaoAuthUrl = new StringBuilder(String.format(
                "https://kauth.kakao.com/oauth/authorize?client_id=%s&redirect_uri=%s&response_type=code",
                kakaoClientId,
                kakaoRedirectUri
        ));

        String state = encodeState(frontendRedirect, role);
        if (state != null) {
            kakaoAuthUrl.append("&state=")
                    .append(URLEncoder.encode(state, StandardCharsets.UTF_8));
        }

        return "redirect:" + kakaoAuthUrl;
    }

    @GetMapping("/kakao/callback")
    @Operation(
            summary = "Kakao OAuth 콜백",
            description = "Kakao에서 리다이렉트되는 콜백 엔드포인트. 이미 가입된 유저라면 JWT를 즉시 발급하고, 신규 가입 유저라면 가입에 필요한 소셜 정보를 반환합니다."
    )
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> kakaoCallback(
            @Parameter(description = "Kakao OAuth 인증 코드", required = true)
            @RequestParam String code,
            @Parameter(description = "Kakao OAuth state")
            @RequestParam(required = false) String state
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

        // 3. 사용자 존재 여부 및 기존 토큰 확인하여 신규/기존 사용자 구분
        java.util.Optional<User> existingUser = userService.findBySocialLoginId(String.valueOf(kakaoId));
        boolean isNew = existingUser.isEmpty() || existingUser.get().getKakaoAccessToken() == null;

        // 4. Kakao 사용자 정보 기반 조회 또는 생성 완결 (즉시 가입 처리)
        User user = userService.findOrCreateKakaoUser(nickname, email, kakaoId, kakaoAccessToken);


        // 5. 우리 서비스 전용 JWT 즉시 발급 및 응답 구성
        String accessToken = jwtProvider.generateAccessToken(user);
        String refreshToken = jwtProvider.generateRefreshToken(user);

        log.info("[Auth] Kakao user authenticated and logged in. User: {}, isNew: {}", user.getId(), isNew);

        KakaoCallbackResponse response = new KakaoCallbackResponse(
                true,
                isNew,
                kakaoAccessToken,
                accessToken,
                refreshToken,
                new KakaoCallbackResponse.UserDto(user.getId())
        );

        Map<String, String> stateMap = decodeState(state);
        String frontendRedirectFromState = stateMap.get("frontendRedirect");
        if (frontendRedirectFromState != null && !frontendRedirectFromState.trim().isBlank()) {
            String frontendRedirect = frontendRedirectFromState.trim();
            String role = stateMap.getOrDefault("role", "pm").trim();
            if (role.isBlank()) {
                role = "pm";
            }

            String redirectUrl = UriComponentsBuilder.fromUriString(frontendRedirect)
                    .queryParam("success", "true")
                    .queryParam("provider", "kakao")
                    .queryParam("kakaoAccessToken", response.kakaoAccessToken())
                    .queryParam("accessToken", response.accessToken())
                    .queryParam("refreshToken", response.refreshToken())
                    .queryParam("userId", response.user().id())
                    .queryParam("username", nickname != null ? nickname : "")
                    .queryParam("email", email != null ? email : "")
                    .queryParam("role", role)
                    .queryParam("isNew", isNew)
                    .build()
                    .encode()
                    .toUriString();

            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(redirectUrl))
                    .build();
        }

        return ResponseEntity.ok(response);
    }

    private String encodeState(String frontendRedirect, String role) {
        if ((frontendRedirect == null || frontendRedirect.isBlank()) &&
                (role == null || role.isBlank())) {
            return null;
        }

        StringBuilder stateBuilder = new StringBuilder();
        if (frontendRedirect != null && !frontendRedirect.isBlank()) {
            stateBuilder
                    .append("frontendRedirect=")
                    .append(URLEncoder.encode(frontendRedirect, StandardCharsets.UTF_8));
        }
        if (role != null && !role.isBlank()) {
            if (stateBuilder.length() > 0) {
                stateBuilder.append("&");
            }
            stateBuilder
                    .append("role=")
                    .append(URLEncoder.encode(role, StandardCharsets.UTF_8));
        }
        return stateBuilder.toString();
    }

    private Map<String, String> decodeState(String state) {
        Map<String, String> result = new HashMap<>();
        if (state == null || state.isBlank()) {
            return result;
        }

        String decodedState = URLDecoder.decode(state, StandardCharsets.UTF_8);
        String[] pairs = decodedState.split("&");
        for (String pair : pairs) {
            String[] entry = pair.split("=", 2);
            if (entry.length != 2) {
                continue;
            }
            result.put(entry[0], URLDecoder.decode(entry[1], StandardCharsets.UTF_8));
        }
        return result;
    }

    @PostMapping("/signup")
    @ResponseBody
    @Operation(
            summary = "신규 회원가입 완료 및 로그인",
            description = "사용자가 직접 입력한 이름/이메일/직군과 소셜 연동 정보를 입력받아 데이터베이스에 유저 정보를 신규 생성하고, 우리 서비스 전용 JWT 토큰을 발급합니다."
    )
    public ResponseEntity<SignupResponse> signup(
            @Valid @RequestBody SignupRequest request
    ) {
        try {
            User user = userService.registerUser(
                    request.email(),
                    request.username(),
                    request.jobRole(),
                    request.socialLoginId(),
                    request.socialType(),
                    request.oauthAccessToken()
            );
            log.info("[Auth] User registered and signup completed: id={}, email={}, username={}, jobRole={}", 
                    user.getId(), user.getEmail(), user.getUsername(), user.getJobRole());

            // 가입 후 즉시 로그인 처리 (JWT 토큰 발급)
            String accessToken = jwtProvider.generateAccessToken(user);
            String refreshToken = jwtProvider.generateRefreshToken(user);

            return ResponseEntity.ok(new SignupResponse(
                    true,
                    "회원가입이 성공적으로 완료되었습니다.",
                    accessToken,
                    refreshToken
            ));

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new SignupResponse(
                    false,
                    e.getMessage(),
                    null,
                    null
            ));
        }
    }

    @GetMapping("/token")
    @ResponseBody
    @Operation(
            summary = "세션에 발급된 JWT 토큰 조회",
            description = "OAuth2 성공 후 세션에 임시 저장된 JWT 토큰을 화면에 JSON으로 반환합니다."
    )
    public SessionTokenResponse getSessionToken(jakarta.servlet.http.HttpSession session) {
        String token = (String) session.getAttribute("jwt_token");
        Long userId = (Long) session.getAttribute("user_id");
        String username = (String) session.getAttribute("username");
        String jobRole = (String) session.getAttribute("job_role");

        if (token != null) {
            return SessionTokenResponse.success(
                    token,
                    userId != null ? userId : "",
                    username != null ? username : "",
                    jobRole != null ? jobRole : ""
            );
        } else {
            return SessionTokenResponse.fail("No token found in session. Please login first.");
        }
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
    public ResponseEntity<DevTokenResponse> devToken(
            @Parameter(description = "토큰을 발급할 사용자 ID", required = true)
            @RequestParam Long userId
    ) {
        return userService.findById(userId)
                .map(user -> {
                    String accessToken = jwtProvider.generateAccessToken(user);
                    return ResponseEntity.ok(DevTokenResponse.success(accessToken, user.getId(), user.getUsername()));
                })
                .orElseGet(() -> {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(DevTokenResponse.fail("User not found: " + userId));
                });
    }

    @PutMapping("/user/job-role")
    @ResponseBody
    @Operation(
            summary = "사용자 직군(JobRole) 설정/수정",
            description = "로그인된 사용자의 직군(PLANNER, FRONTEND, BACKEND, AI)을 설정하거나 수정합니다."
    )
    public JobRoleUpdateResponse updateJobRole(
            @Parameter(description = "설정할 직군 (PLANNER, FRONTEND, BACKEND, AI)", required = true)
            @RequestParam JobRole jobRole,
            org.springframework.security.core.Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return JobRoleUpdateResponse.fail("User is not authenticated.");
        }

        String username = authentication.getName();
        java.util.Optional<User> userOpt = userService.findByUsername(username);

        if (userOpt.isEmpty()) {
            userOpt = userService.findBySocialLoginId(username);
        }

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            userService.updateJobRole(user.getId(), jobRole);
            return JobRoleUpdateResponse.success(
                    "Job role updated successfully.",
                    user.getId(),
                    user.getUsername(),
                    user.getEmail() != null ? user.getEmail() : "",
                    jobRole.name()
            );
        } else {
            return JobRoleUpdateResponse.fail("User not found.");
        }
    }
}
