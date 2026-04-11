package markoala.fithub.demo.auth.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import markoala.fithub.demo.domain.user.entity.User;
import markoala.fithub.demo.domain.user.service.UserService;

@Service
@RequiredArgsConstructor
public class GithubWebClientService {

    private static final Logger log = LoggerFactory.getLogger(GithubWebClientService.class);

    private final UserService userService;

    public GithubAuthInfo getAuthInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        log.info("현재 Authentication 타입: {}", authentication != null ? authentication.getClass().getName() : "null");

        if (authentication != null && authentication.isAuthenticated()) {
            // JWT 토큰에서 사용자명 추출
            String username = authentication.getName();
            
            // 사용자명으로 User 엔티티 조회
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다: " + username));
            
            String accessToken = user.getGithubAccessToken();
            if (accessToken == null) {
                throw new IllegalStateException("GitHub access token이 저장되지 않았습니다. 다시 로그인해주세요.");
            }

            return new GithubAuthInfo(username, accessToken);
        }

        throw new IllegalStateException("인증이 필요합니다.");
    }

    public WebClient getWebClient(String accessToken) {
        return WebClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .build();
    }

    public record GithubAuthInfo(String username, String accessToken) {}
}
