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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "GitHub OAuth 인증 API")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final GithubRepositoryService githubRepositoryService;
    private final UserService userService;
    private final JwtProvider jwtProvider;

    @Value("${github.client-id}")
    private String githubClientId;

    @Value("${github.client-secret}")
    private String githubClientSecret;

    @Value("${github.redirect-uri}")
    private String githubRedirectUri;

    public AuthController(
            GithubRepositoryService githubRepositoryService,
            UserService userService,
            JwtProvider jwtProvider
    ) {
        this.githubRepositoryService = githubRepositoryService;
        this.userService = userService;
        this.jwtProvider = jwtProvider;
    }

    @GetMapping("/login")
    @Operation(
            summary = "GitHub OAuth 로그인",
            description = "GitHub OAuth 인증 URL을 반환합니다. 프론트에서 이 URL로 리다이렉트하면 됩니다"
    )
    public ResponseEntity<?> login() {
        log.info("[Auth] Generating GitHub OAuth URL");

        String githubAuthUrl = String.format(
                "https://github.com/login/oauth/authorize?client_id=%s&redirect_uri=%s&scope=repo,user",
                githubClientId,
                githubRedirectUri
        );

        Map<String, Object> response = new HashMap<>();
        response.put("authUrl", githubAuthUrl);
        response.put("message", "프론트엔드에서 이 URL로 리다이렉트하세요");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/github/callback")
    @Operation(
            summary = "GitHub OAuth 콜백",
            description = "GitHub에서 리다이렉트되는 콜백 엔드포인트. JWT 토큰을 발급하고 JSON으로 응답합니다"
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

        // 5. 토큰을 JSON으로 응답 (프론트에서 처리)
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("accessToken", accessToken);
        response.put("refreshToken", refreshToken);
        response.put("user", Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail()
        ));
        response.put("redirectUrl", "http://localhost:3000/dashboard");

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
