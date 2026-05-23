package markoala.fithub.demo.global.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import markoala.fithub.demo.user.User;
import markoala.fithub.demo.user.UserService;
import markoala.fithub.demo.user.JobRole;
import markoala.fithub.demo.global.security.jwt.JwtProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    public static final String SESSION_KEY_TOKEN = "jwt_token";
    public static final String SESSION_KEY_USER_ID = "user_id";
    public static final String SESSION_KEY_USERNAME = "username";

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OAuth2SuccessHandler.class);

    private final JwtProvider tokenProvider;
    private final UserService userService;
    private final OAuth2AuthorizedClientService authorizedClientService;
    @Value("${app.frontend.oauth-callback-url:http://localhost:3000/auth/oauth/callback}")
    private String frontendOAuthCallbackUrl;

    @Override
    @SuppressWarnings("unchecked")
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        try {
            log.info("[OAuth2SuccessHandler] Authentication success callback triggered");
            
            // OAuth2User 정보 추출
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            log.info("[OAuth2SuccessHandler] OAuth2User attributes: {}", oauth2User.getAttributes());
            
            String provider = "github";
            if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
                provider = oauthToken.getAuthorizedClientRegistrationId();
            }
            log.info("[OAuth2SuccessHandler] Detected OAuth provider: {}", provider);

            String login = null;
            String email = null;
            String socialLoginId = null;
            String socialAccessToken = null;

            // access token 추출
            if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
                OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                        oauthToken.getAuthorizedClientRegistrationId(),
                        oauthToken.getName()
                );

                if (client != null && client.getAccessToken() != null) {
                    socialAccessToken = client.getAccessToken().getTokenValue();
                }
            }

            User user;

            if ("kakao".equalsIgnoreCase(provider)) {
                // Kakao attributes
                Object idObj = oauth2User.getAttribute("id");
                socialLoginId = idObj != null ? String.valueOf(idObj) : null;

                java.util.Map<String, Object> properties = (java.util.Map<String, Object>) oauth2User.getAttribute("properties");
                if (properties != null) {
                    login = (String) properties.get("nickname");
                }

                java.util.Map<String, Object> kakaoAccount = (java.util.Map<String, Object>) oauth2User.getAttribute("kakao_account");
                if (kakaoAccount != null) {
                    email = (String) kakaoAccount.get("email");
                }

                log.info("[OAuth2SuccessHandler] Parsed Kakao credentials: id={}, login={}, email={}", socialLoginId, login, email);

                Long kakaoIdLong = socialLoginId != null ? Long.parseLong(socialLoginId) : null;
                user = userService.findOrCreateKakaoUser(login, email, kakaoIdLong, socialAccessToken);
            } else {
                // GitHub attributes
                login = (String) oauth2User.getAttribute("login");
                email = (String) oauth2User.getAttribute("email");
                Object idObj = oauth2User.getAttribute("id");
                socialLoginId = idObj != null ? String.valueOf(((Number) idObj).longValue()) : null;

                log.info("[OAuth2SuccessHandler] Parsed GitHub credentials: id={}, login={}, email={}", socialLoginId, login, email);

                Long githubIdLong = socialLoginId != null ? Long.parseLong(socialLoginId) : null;
                user = userService.findOrCreateGithubUser(login, email, githubIdLong, socialAccessToken);
            }

            log.info("[OAuth2SuccessHandler] User retrieval/creation succeeded. user_id: {}", user.getId());

            // JWT 토큰 생성
            String appAccessToken = tokenProvider.generateAccessToken(user);
            String refreshToken = tokenProvider.generateRefreshToken(user);

            // 레거시 호환: 세션에도 저장
            HttpSession session = request.getSession();
            session.setAttribute(SESSION_KEY_TOKEN, appAccessToken);
            session.setAttribute(SESSION_KEY_USER_ID, user.getId());
            session.setAttribute(SESSION_KEY_USERNAME, user.getUsername());
            session.setAttribute("job_role", user.getJobRole() != null ? user.getJobRole().name() : "");

            String appRole = resolveAppRole(provider, user.getJobRole());
            String emailValue = user.getEmail() != null ? user.getEmail() : "";

            UriComponentsBuilder redirectBuilder = UriComponentsBuilder
                    .fromUriString(frontendOAuthCallbackUrl)
                    .queryParam("success", "true")
                    .queryParam("provider", provider)
                    .queryParam("accessToken", appAccessToken)
                    .queryParam("refreshToken", refreshToken)
                    .queryParam("userId", user.getId())
                    .queryParam("username", user.getUsername())
                    .queryParam("email", emailValue)
                    .queryParam("role", appRole);

            if ("kakao".equalsIgnoreCase(provider)) {
                redirectBuilder.queryParam("kakaoAccessToken", socialAccessToken != null ? socialAccessToken : "");
            } else {
                redirectBuilder
                        .queryParam("gitAccessToken", socialAccessToken != null ? socialAccessToken : "")
                        .queryParam("githubAccessToken", socialAccessToken != null ? socialAccessToken : "");
            }

            String redirectUrl = redirectBuilder.build().encode().toUriString();

            log.info("[OAuth2SuccessHandler] Session JWT created. Redirecting to FE callback: {}", redirectUrl);
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);

        } catch (Exception e) {
            log.error("[OAuth2SuccessHandler] CRITICAL ERROR: Failed to process OAuth2 authentication success!", e);
            throw new RuntimeException("OAuth2 Success Handler processing failed", e);
        }
    }

    private String resolveAppRole(String provider, JobRole jobRole) {
        if (jobRole != null) {
            return switch (jobRole) {
                case PLANNER -> "pm";
                case FRONTEND -> "dev-fe";
                case BACKEND -> "dev-be";
                case AI -> "dev-fe";
            };
        }

        if ("kakao".equalsIgnoreCase(provider)) {
            return "pm";
        }
        return "dev-fe";
    }
}
