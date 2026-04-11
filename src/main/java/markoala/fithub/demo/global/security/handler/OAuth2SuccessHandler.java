package markoala.fithub.demo.global.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import markoala.fithub.demo.domain.user.entity.User;
import markoala.fithub.demo.domain.user.service.UserService;
import markoala.fithub.demo.global.security.jwt.JwtTokenProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    public static final String SESSION_KEY_TOKEN   = "jwt_token";
    public static final String SESSION_KEY_USER_ID = "jwt_user_id";
    public static final String SESSION_KEY_USERNAME = "jwt_username";

    private final JwtTokenProvider tokenProvider;
    private final UserService userService;
    private final OAuth2AuthorizedClientService authorizedClientService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String login = (String) oauth2User.getAttribute("login");
        String email = (String) oauth2User.getAttribute("email");

        Object idObj = oauth2User.getAttribute("id");
        String id = idObj != null ? String.valueOf(((Number) idObj).longValue()) : null;

        User user = userService.findOrCreateGitHubUser(login, email, id);

        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                    oauthToken.getAuthorizedClientRegistrationId(),
                    oauthToken.getName()
            );
            if (client != null && client.getAccessToken() != null) {
                user.updateGithubAccessToken(client.getAccessToken().getTokenValue());
                userService.save(user);
            }
        }

        String token = tokenProvider.createToken(authentication);

        // 세션에 JWT 임시 저장 → /api/v1/auth/token 에서 JSON으로 꺼내줌
        HttpSession session = request.getSession();
        session.setAttribute(SESSION_KEY_TOKEN,   token);
        session.setAttribute(SESSION_KEY_USER_ID, user.getId());
        session.setAttribute(SESSION_KEY_USERNAME, user.getUsername());

        getRedirectStrategy().sendRedirect(request, response, "/api/v1/auth/token");
    }
}
