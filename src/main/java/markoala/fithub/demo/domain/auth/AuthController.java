package markoala.fithub.demo.domain.auth;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import markoala.fithub.demo.domain.auth.dto.SignupRequest;
import markoala.fithub.demo.domain.auth.dto.GithubCallbackResponse;
import markoala.fithub.demo.domain.auth.dto.KakaoCallbackResponse;
import markoala.fithub.demo.domain.auth.dto.SessionTokenResponse;
import markoala.fithub.demo.domain.auth.dto.SignupResponse;
import markoala.fithub.demo.domain.auth.dto.JobRoleUpdateResponse;
import markoala.fithub.demo.domain.auth.dto.DevTokenResponse;
import markoala.fithub.demo.global.security.jwt.JwtProvider;
import markoala.fithub.demo.domain.github.service.GithubRepositoryService;
import markoala.fithub.demo.domain.user.User;
import markoala.fithub.demo.domain.user.UserService;
import markoala.fithub.demo.domain.user.JobRole;
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
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "GitHub/Kakao OAuth 소셜 로그인 및 회원가입 API. "
        + "두 가지 소셜 로그인(GitHub, Kakao)을 지원하며, 로그인 성공 시 서비스 전용 JWT 토큰을 발급합니다.")
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
            summary = "GitHub OAuth 로그인 시작",
            description = """
                    **GitHub 소셜 로그인의 시작점입니다.**
                    
                    이 엔드포인트를 호출하면 GitHub OAuth 인증 페이지로 자동 리다이렉트됩니다.
                    사용자가 GitHub에서 인증을 완료하면 숨김 처리된 `/auth/github/callback`으로 콜백됩니다.
                    현재 콜백은 `isNew`, `accessToken`, `refreshToken` JSON 응답을 발급합니다.
                    
                    ### 사용 방법
                    1. **브라우저에서** 이 URL을 직접 열거나 `<a>` 태그로 연결합니다
                    2. `frontendRedirect`, `role` 파라미터는 OAuth `state`에 보존됩니다
                    3. GitHub 인증 완료 → 콜백 → JWT 발급
                    
                    ### 호출 예시
                    ```
                    GET /auth/login?frontendRedirect=http://localhost:3000/auth/callback&role=dev-fe
                    ```
                    
                    ### 콜백 JSON 응답 예시
                    ```json
                    {
                      "isNew": true,
                      "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
                      "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
                    }
                    ```
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "GitHub OAuth 인증 페이지로 리다이렉트")
    })
    public String login(
            @Parameter(description = "로그인 성공 후 프론트엔드 리다이렉트 URL (예: http://localhost:3000/auth/callback). "
                    + "지정하면 콜백 완료 후 이 URL로 JWT 토큰과 함께 리다이렉트됩니다. "
                    + "미지정 시 콜백에서 JSON 응답을 반환합니다.",
                    example = "http://localhost:3000/auth/callback")
            @RequestParam(required = false) String frontendRedirect,
            @Parameter(description = "사용자 역할 (pm, dev-fe, dev-be). "
                    + "프론트엔드 리다이렉트 시 role 파라미터로 전달됩니다. 기본값: dev-fe",
                    example = "dev-fe")
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

    @Hidden
    @GetMapping("/github/callback")
    @Operation(
            summary = "GitHub OAuth 콜백 (자동 호출)",
            description = """
                    **GitHub OAuth 인증 완료 후 자동으로 호출되는 콜백 엔드포인트입니다. 직접 호출하지 마세요.**
                    
                    ### 내부 처리 로직
                    1. GitHub에서 전달받은 `code`로 GitHub Access Token을 교환합니다
                    2. GitHub Access Token으로 사용자 정보(login, email, id)를 조회합니다
                    3. DB에서 기존 사용자를 찾거나 신규 생성합니다 (`findOrCreateGithubUser`)
                    4. 서비스 전용 JWT `accessToken`과 `refreshToken`을 발급합니다
                    
                    ### 응답 방식
                    - **`frontendRedirect`가 state에 포함된 경우**: 302 리다이렉트로 프론트엔드에 JWT 전달
                    - **`frontendRedirect`가 없는 경우**: 200 OK + JSON 응답
                    
                    ### JSON 응답 예시 (frontendRedirect 미사용 시)
                    ```json
                    {
                      "isNew": false,
                      "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
                      "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
                    }
                    ```
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "JSON 응답 (frontendRedirect 미사용 시)",
                    content = @Content(schema = @Schema(implementation = GithubCallbackResponse.class))),
            @ApiResponse(responseCode = "302", description = "프론트엔드 URL로 리다이렉트 (frontendRedirect 사용 시)"),
            @ApiResponse(responseCode = "500", description = "GitHub API 호출 실패 또는 토큰 교환 실패")
    })
    public ResponseEntity<?> githubCallback(
            @Parameter(description = "GitHub에서 자동 전달하는 OAuth 인증 코드", required = true)
            @RequestParam String code,
            @Parameter(description = "로그인 시작 시 전달한 state (frontendRedirect, role 정보 인코딩). GitHub에서 자동 전달")
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

        // 3. 가입 완료 여부(isNew)를 토큰 존재 여부로 판단
        boolean isNew = !userService.hasGithubAccessToken(githubId);

        // 4. GitHub 사용자 정보 기반 조회 또는 생성 완결 (즉시 가입 처리)
        User user = userService.findOrCreateGithubUser(githubLogin, githubEmail, githubId, githubAccessToken);

        // 5. 우리 서비스 전용 JWT 즉시 발급 및 응답 구성
        String accessToken = jwtProvider.generateAccessToken(user);
        String refreshToken = jwtProvider.generateRefreshToken(user);

        log.info("[Auth] GitHub user authenticated and logged in. User: {}, isNew: {}", user.getId(), isNew);

        GithubCallbackResponse response = new GithubCallbackResponse(
                isNew,
                accessToken,
                refreshToken
        );

        String frontendRedirect = decodeState(state).get("frontendRedirect");
        if (frontendRedirect != null && !frontendRedirect.isBlank()) {
            return redirectWithTokens(frontendRedirect, isNew, accessToken, refreshToken);
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/kakao/login")
    @Operation(
            summary = "Kakao OAuth 로그인 시작",
            description = """
                    **Kakao 소셜 로그인의 시작점입니다.**
                    
                    이 엔드포인트를 호출하면 Kakao OAuth 인증 페이지로 자동 리다이렉트됩니다.
                    사용자가 Kakao에서 인증을 완료하면 숨김 처리된 `/auth/kakao/callback`으로 콜백됩니다.
                    현재 콜백은 `isNew`, `accessToken`, `refreshToken` JSON 응답을 발급합니다.
                    
                    ### 사용 방법
                    1. **브라우저에서** 이 URL을 직접 열거나 `<a>` 태그로 연결합니다
                    2. `frontendRedirect`, `role` 파라미터는 OAuth `state`에 보존됩니다
                    3. Kakao 인증 완료 → 콜백 → JWT 발급
                    
                    ### 호출 예시
                    ```
                    GET /auth/kakao/login?frontendRedirect=http://localhost:3000/auth/callback&role=pm
                    ```
                    
                    ### 콜백 JSON 응답 예시
                    ```json
                    {
                      "isNew": true,
                      "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
                      "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
                    }
                    ```
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Kakao OAuth 인증 페이지로 리다이렉트")
    })
    public String kakaoLogin(
            @Parameter(description = "로그인 성공 후 프론트엔드 리다이렉트 URL (예: http://localhost:3000/auth/callback). "
                    + "지정하면 콜백 완료 후 이 URL로 JWT 토큰과 함께 리다이렉트됩니다. "
                    + "미지정 시 콜백에서 JSON 응답을 반환합니다.",
                    example = "http://localhost:3000/auth/callback")
            @RequestParam(required = false) String frontendRedirect,
            @Parameter(description = "사용자 역할 (pm, dev-fe, dev-be). "
                    + "프론트엔드 리다이렉트 시 role 파라미터로 전달됩니다. 기본값: pm",
                    example = "pm")
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

    @Hidden
    @GetMapping("/kakao/callback")
    @Operation(
            summary = "Kakao OAuth 콜백 (자동 호출)",
            description = """
                    **Kakao OAuth 인증 완료 후 자동으로 호출되는 콜백 엔드포인트입니다. 직접 호출하지 마세요.**
                    
                    ### 내부 처리 로직
                    1. Kakao에서 전달받은 `code`로 Kakao Access Token을 교환합니다
                    2. Kakao Access Token으로 사용자 정보(id, nickname, email)를 조회합니다
                    3. DB에서 기존 사용자를 찾거나 신규 생성합니다 (`findOrCreateKakaoUser`)
                    4. 서비스 전용 JWT `accessToken`과 `refreshToken`을 발급합니다
                    
                    ### 응답 방식
                    - **`frontendRedirect`가 state에 포함된 경우**: 302 리다이렉트로 프론트엔드에 JWT 전달
                    - **`frontendRedirect`가 없는 경우**: 200 OK + JSON 응답
                    
                    ### JSON 응답 예시 (frontendRedirect 미사용 시)
                    ```json
                    {
                      "isNew": false,
                      "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
                      "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
                    }
                    ```
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "JSON 응답 (frontendRedirect 미사용 시)",
                    content = @Content(schema = @Schema(implementation = KakaoCallbackResponse.class))),
            @ApiResponse(responseCode = "302", description = "프론트엔드 URL로 리다이렉트 (frontendRedirect 사용 시)"),
            @ApiResponse(responseCode = "500", description = "Kakao API 호출 실패 또는 토큰 교환 실패")
    })
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> kakaoCallback(
            @Parameter(description = "Kakao에서 자동 전달하는 OAuth 인증 코드", required = true)
            @RequestParam String code,
            @Parameter(description = "로그인 시작 시 전달한 state (frontendRedirect, role 정보 인코딩). Kakao에서 자동 전달")
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

        // 3. 가입 완료 여부(isNew)를 토큰 존재 여부로 판단
        boolean isNew = !userService.hasKakaoAccessToken(kakaoId);

        // 4. Kakao 사용자 정보 기반 조회 또는 생성 완결 (즉시 가입 처리)
        User user = userService.findOrCreateKakaoUser(nickname, email, kakaoId, kakaoAccessToken);

        // 5. 우리 서비스 전용 JWT 즉시 발급 및 응답 구성
        String accessToken = jwtProvider.generateAccessToken(user);
        String refreshToken = jwtProvider.generateRefreshToken(user);

        log.info("[Auth] Kakao user authenticated and logged in. User: {}, isNew: {}", user.getId(), isNew);

        KakaoCallbackResponse response = new KakaoCallbackResponse(
                isNew,
                accessToken,
                refreshToken
        );

        String frontendRedirect = decodeState(state).get("frontendRedirect");
        if (frontendRedirect != null && !frontendRedirect.isBlank()) {
            return redirectWithTokens(frontendRedirect, isNew, accessToken, refreshToken);
        }

        return ResponseEntity.ok(response);
    }

    private ResponseEntity<?> redirectWithTokens(String frontendRedirect, boolean isNew, String accessToken, String refreshToken) {
        String redirectUrl = frontendRedirect
                + "#isNew=" + isNew
                + "&accessToken=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8)
                + "&refreshToken=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirectUrl))
                .build();
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

    @Hidden
    @PostMapping("/signup")
    @ResponseBody
    @Operation(
            summary = "신규 회원가입 완료 및 로그인",
            description = """
                    **소셜 로그인 후 신규 사용자의 회원가입을 완료합니다.**
                    
                    GitHub/Kakao 콜백에서 `isNew: true`로 반환된 경우, 이 엔드포인트를 호출하여
                    추가 정보(이름, 이메일, 직군)를 등록하고 JWT 토큰을 발급받습니다.
                    
                    ### 사용 시점
                    1. `/auth/login` 또는 `/auth/kakao/login`으로 소셜 로그인 수행
                    2. 콜백 응답에서 `isNew: true` 확인
                    3. 이 엔드포인트를 호출하여 회원가입 완료
                    
                    ### Request Body
                    ```json
                    {
                      "email": "user@example.com",
                      "username": "홍길동",
                      "jobRole": "BACKEND",
                      "socialLoginId": "12345678",
                      "socialType": "GITHUB",
                      "oauthAccessToken": "gho_xxxxxxxxxxxx"
                    }
                    ```
                    
                    - `jobRole`: PLANNER, FRONTEND, BACKEND, AI 중 택1
                    - `socialType`: GITHUB 또는 KAKAO
                    - `socialLoginId`: 콜백에서 받은 소셜 고유 ID
                    - `oauthAccessToken`: 콜백에서 받은 소셜 Access Token (gitAccessToken 또는 kakaoAccessToken)
                    """
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

    @Hidden
    @GetMapping("/token")
    @ResponseBody
    @Operation(
            summary = "세션에 발급된 JWT 토큰 조회 (레거시)",
            description = """
                    **[레거시] 세션에 저장된 JWT 토큰을 조회합니다.**
                    
                    현재는 OAuth 콜백에서 직접 JWT를 반환하므로, 이 엔드포인트는 레거시 호환용입니다.
                    일반적으로는 콜백의 JSON 응답 또는 리다이렉트 쿼리 파라미터에서 토큰을 수신합니다.
                    """
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
    @Hidden
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
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(DevTokenResponse.fail("사용자를 찾을 수 없습니다: " + userId));
                });
    }

    @Hidden
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
            return JobRoleUpdateResponse.fail("사용자를 찾을 수 없습니다.");
        }
    }
}
