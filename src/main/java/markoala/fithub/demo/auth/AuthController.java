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
}
