package markoala.fithub.demo.github.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import markoala.fithub.demo.auth.GithubWebClientService;
import markoala.fithub.demo.github.dto.GithubRepositoryDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GithubRepositoryService {

    private static final Logger log = LoggerFactory.getLogger(GithubRepositoryService.class);

    private final GithubWebClientService githubWebClientService;

    @Value("${github.client-id}")
    private String githubClientId;

    @Value("${github.client-secret}")
    private String githubClientSecret;

    public List<GithubRepositoryDto> getMyRepos() {
        var auth = githubWebClientService.getAuthInfo();
        var webClient = githubWebClientService.getWebClient(auth.accessToken());

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/user/repos")
                        .queryParam("per_page", "100")
                        .queryParam("sort", "created")
                        .queryParam("direction", "desc")
                        .build())
                .retrieve()
                .bodyToFlux(GithubRepositoryDto.class)
                .collectList()
                .block();
    }

    /**
     * GitHub OAuth 인증 코드를 access token으로 교환
     */
    public String exchangeCodeForToken(String code) {
        log.info("[GitHub] Exchanging authorization code for access token");

        WebClient webClient = WebClient.builder()
                .baseUrl("https://github.com")
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("client_id", githubClientId);
        requestBody.put("client_secret", githubClientSecret);
        requestBody.put("code", code);

        Map<String, Object> response = webClient.post()
                .uri("/login/oauth/access_token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || response.containsKey("error")) {
            log.error("[GitHub] Failed to exchange code for token: {}",
                response != null ? response.get("error_description") : "Unknown error");
            throw new RuntimeException("GitHub OAuth token exchange failed");
        }

        String accessToken = (String) response.get("access_token");
        log.info("[GitHub] Successfully obtained access token");
        return accessToken;
    }

    /**
     * GitHub API를 통해 사용자 정보 조회
     */
    public Map<String, Object> getUserInfoFromGithub(String githubAccessToken) {
        log.info("[GitHub] Fetching user information from GitHub API");

        WebClient webClient = WebClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + githubAccessToken)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();

        Map<String, Object> userInfo = webClient.get()
                .uri("/user")
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (userInfo == null) {
            log.error("[GitHub] Failed to fetch user information");
            throw new RuntimeException("Failed to fetch GitHub user information");
        }

        log.info("[GitHub] Successfully fetched user info for: {}", userInfo.get("login"));
        return userInfo;
    }
}
