package markoala.fithub.demo.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class GithubWebClientService {

    private final OAuth2AuthorizedClientService authorizedClientService;

    public GithubAuthInfo getAuthInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 로그를 찍어 현재 인증 상태를 확인합니다.
        log.info("현재 Authentication 타입: {}", authentication != null ? authentication.getClass().getName() : "null");

        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                    oauthToken.getAuthorizedClientRegistrationId(),
                    oauthToken.getName()
            );

            if (client == null) {
                log.error("AuthorizedClient를 찾을 수 없습니다. RegistrationId: {}, Name: {}",
                        oauthToken.getAuthorizedClientRegistrationId(), oauthToken.getName());
                throw new IllegalStateException("OAuth2 클라이언트 정보를 로드할 수 없습니다.");
            }

            String accessToken = client.getAccessToken().getTokenValue();
            String username = oauthToken.getPrincipal().getAttribute("login");

            return new GithubAuthInfo(username, accessToken);
        }

        // 이 에러가 난다면 SecurityConfig에서 /home을 제대로 막지 않은 것입니다.
        throw new IllegalStateException("OAuth2 인증이 필요합니다. (현재 인증 객체 없음)");
    }

    public WebClient getWebClient(String accessToken) {
        return WebClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .build();
    }

    public record GithubAuthInfo(String username, String accessToken) {}
}
