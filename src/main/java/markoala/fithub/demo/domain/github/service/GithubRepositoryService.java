package markoala.fithub.demo.domain.github.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import markoala.fithub.demo.domain.auth.GithubWebClientService;
import markoala.fithub.demo.domain.github.dto.GithubRepositoryDto;
import markoala.fithub.demo.domain.user.User;
import markoala.fithub.demo.domain.user.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GithubRepositoryService {

    private static final Logger log = LoggerFactory.getLogger(GithubRepositoryService.class);

    private final GithubWebClientService githubWebClientService;
    private final UserService userService;

    @Value("${github.client-id}")
    private String githubClientId;

    @Value("${github.client-secret}")
    private String githubClientSecret;

    @Value("${github.redirect-uri}")
    private String githubRedirectUri;

    public List<GithubRepositoryDto> getMyRepos() {
        var auth = githubWebClientService.getAuthInfo();
        return getMyReposByAccessToken(auth.accessToken());
    }

    public List<GithubRepositoryDto> getMyRepos(Long userId) {
        return getMyReposByAccessToken(getGithubAccessToken(userId));
    }

    public GithubRepositoryDto getRepoDetail(Long userId, Long repoId) {
        String githubAccessToken = getGithubAccessToken(userId);
        var webClient = githubWebClientService.getWebClient(githubAccessToken);

        try {
            GithubRepositoryDto repo = webClient.get()
                    .uri("/repositories/{repoId}", repoId)
                    .retrieve()
                    .bodyToMono(GithubRepositoryDto.class)
                    .block();

            if (repo == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "레포지토리를 찾을 수 없습니다.");
            }

            return repo;
        } catch (WebClientResponseException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "레포지토리를 찾을 수 없습니다.", e);
        }
    }

    private String getGithubAccessToken(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }

        User user = userService.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다: " + userId));

        String githubAccessToken = user.getGithubAccessToken();
        if (githubAccessToken == null || githubAccessToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "GitHub access token이 저장되지 않았습니다. GitHub 로그인 후 다시 시도해주세요.");
        }

        return githubAccessToken;
    }

    private List<GithubRepositoryDto> getMyReposByAccessToken(String githubAccessToken) {
        var webClient = githubWebClientService.getWebClient(githubAccessToken);

        List<GithubRepositoryDto> repos = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/user/repos")
                        .queryParam("per_page", "100")
                        .queryParam("affiliation", "owner,collaborator,organization_member")
                        .queryParam("sort", "updated")
                        .queryParam("direction", "desc")
                        .build())
                .retrieve()
                .bodyToFlux(GithubRepositoryDto.class)
                .collectList()
                .block();

        return new ArrayList<>(repos != null ? repos : List.<GithubRepositoryDto>of()).stream()
                .collect(Collectors.toMap(
                        GithubRepositoryDto::id,
                        repo -> repo,
                        (existing, replacement) -> existing
                ))
                .values()
                .stream()
                .sorted((a, b) -> String.valueOf(b.updatedAt()).compareTo(String.valueOf(a.updatedAt())))
                .collect(Collectors.toList());
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

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("client_id", githubClientId);
        requestBody.add("client_secret", githubClientSecret);
        requestBody.add("code", code);
        requestBody.add("redirect_uri", githubRedirectUri);

        Map<String, Object> response = webClient.post()
                .uri("/login/oauth/access_token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(requestBody))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || response.containsKey("error")) {
            log.error("[GitHub] Failed to exchange code for token: {}",
                response != null ? response.get("error_description") : "Unknown error");
            throw new RuntimeException("GitHub OAuth 토큰 발급에 실패했습니다.");
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
            throw new RuntimeException("GitHub 사용자 정보 조회에 실패했습니다.");
        }

        log.info("[GitHub] Successfully fetched user info for: {}", userInfo.get("login"));
        return userInfo;
    }
}
