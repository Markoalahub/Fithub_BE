package markoala.fithub.demo.global.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import markoala.fithub.demo.user.User;
import markoala.fithub.demo.user.UserService;
import markoala.fithub.demo.global.security.jwt.JwtTokenProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    public static final String SESSION_KEY_TOKEN = "jwt_token";
    public static final String SESSION_KEY_USER_ID = "user_id";
    public static final String SESSION_KEY_USERNAME = "username";

    private final JwtTokenProvider tokenProvider;
    private final UserService userService;
    private final OAuth2AuthorizedClientService authorizedClientService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        // OAuth2User 정보 추출
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String login = (String) oauth2User.getAttribute("login");
        String email = (String) oauth2User.getAttribute("email");
        
        // GitHub ID는 Integer로 반환되므로 Long으로 변환 후 String으로
        Object idObj = oauth2User.getAttribute("id");
        String id = idObj != null ? String.valueOf(((Number) idObj).longValue()) : null;

        // 사용자 조회 또는 생성
        User user = userService.findOrCreateGitHubUser(login, email, id);

        // GitHub access token 저장
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                    oauthToken.getAuthorizedClientRegistrationId(),
                    oauthToken.getName()
            );
            
            if (client != null && client.getAccessToken() != null) {
                String accessToken = client.getAccessToken().getTokenValue();
                user.updateGithubAccessToken(accessToken);
                userService.save(user);
            }
        }

        // JWT 토큰 생성
        String token = tokenProvider.createToken(authentication);

        // JWT를 쿼리 파라미터로 전달
        String targetUrl = UriComponentsBuilder.fromUriString("/home")
                .queryParam("token", token)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
