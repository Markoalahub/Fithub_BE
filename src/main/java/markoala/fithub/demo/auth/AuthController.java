package markoala.fithub.demo.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import markoala.fithub.demo.global.security.jwt.JwtProvider;
import markoala.fithub.demo.github.service.GithubRepositoryService;
import markoala.fithub.demo.user.User;
import markoala.fithub.demo.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseBody;

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
            description = "GitHub에서 리다이렉트되는 콜백 엔드포인트. JWT 토큰, GitHub Access Token을 발급합니다"
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
        String githubEmail = (String) userInfo.get("email");
        Long githubId = ((Number) userInfo.get("id")).longValue();

        log.info("[Auth] GitHub user info: login={}, email={}", githubLogin, githubEmail);

        // 3. DB에 사용자 저장 또는 업데이트
        User user = userService.findOrCreateGithubUser(githubLogin, githubEmail, githubId, githubAccessToken);
        log.info("[Auth] User saved/updated: id={}, username={}", user.getId(), user.getUsername());

        // 4. JWT 토큰 발급
        String accessToken = jwtProvider.generateAccessToken(user);
        String refreshToken = jwtProvider.generateRefreshToken(user);

        log.info("[Auth] JWT tokens generated for user: {}", user.getId());

        // 5. 토큰 정보를 JSON으로 응답
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("accessToken", accessToken);
        response.put("refreshToken", refreshToken);
        response.put("githubAccessToken", githubAccessToken);
        response.put("user", Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail()
        ));

        log.info("[Auth] OAuth callback completed. Tokens issued for user: {}", user.getId());

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
            description = "Kakao에서 리다이렉트되는 콜백 엔드포인트. JWT 토큰, Kakao Access Token을 발급합니다"
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
        String email = kakaoAccount != null ? (String) kakaoAccount.get("email") : null;

        log.info("[Auth] Kakao user info: nickname={}, email={}", nickname, email);

        // 3. DB에 사용자 저장 또는 업데이트
        User user = userService.findOrCreateKakaoUser(nickname, email, kakaoId, kakaoAccessToken);
        log.info("[Auth] User saved/updated: id={}, username={}", user.getId(), user.getUsername());

        // 4. JWT 토큰 발급
        String accessToken = jwtProvider.generateAccessToken(user);
        String refreshToken = jwtProvider.generateRefreshToken(user);

        log.info("[Auth] JWT tokens generated for user: {}", user.getId());

        // 5. 토큰 정보를 JSON으로 응답
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("accessToken", accessToken);
        response.put("refreshToken", refreshToken);
        response.put("kakaoAccessToken", kakaoAccessToken);
        response.put("user", Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail()
        ));

        log.info("[Auth] Kakao OAuth callback completed. Tokens issued for user: {}", user.getId());

        return response;
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

        Map<String, Object> response = new java.util.HashMap<>();
        if (token != null) {
            response.put("success", true);
            response.put("accessToken", token);
            response.put("user", Map.of(
                    "id", userId != null ? userId : "",
                    "username", username != null ? username : ""
            ));
        } else {
            response.put("success", false);
            response.put("message", "No token found in session. Please login first.");
        }
        return response;
    }
}
