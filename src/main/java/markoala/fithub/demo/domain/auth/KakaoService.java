package markoala.fithub.demo.domain.auth;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class KakaoService {

    private static final Logger log = LoggerFactory.getLogger(KakaoService.class);

    @Value("${kakao.client-id}")
    private String kakaoClientId;

    @Value("${kakao.client-secret:}")
    private String kakaoClientSecret;

    @Value("${kakao.redirect-uri}")
    private String kakaoRedirectUri;

    /**
     * Kakao OAuth 인증 코드를 access token으로 교환
     */
    public String exchangeCodeForToken(String code) {
        log.info("[Kakao] Exchanging authorization code for access token");

        WebClient webClient = WebClient.builder()
                .baseUrl("https://kauth.kakao.com")
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("grant_type", "authorization_code");
        requestBody.add("client_id", kakaoClientId);
        if (kakaoClientSecret != null && !kakaoClientSecret.isEmpty()) {
            requestBody.add("client_secret", kakaoClientSecret);
        }
        requestBody.add("redirect_uri", kakaoRedirectUri);
        requestBody.add("code", code);

        Map<String, Object> response = webClient.post()
                .uri("/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(requestBody))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || response.containsKey("error")) {
            log.error("[Kakao] Failed to exchange code for token: {}",
                    response != null ? response.get("error_description") : "Unknown error");
            throw new RuntimeException("카카오 OAuth 토큰 발급에 실패했습니다.");
        }

        String accessToken = (String) response.get("access_token");
        log.info("[Kakao] Successfully obtained access token");
        return accessToken;
    }

    /**
     * Kakao API를 통해 사용자 정보 조회
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getUserInfoFromKakao(String kakaoAccessToken) {
        log.info("[Kakao] Fetching user information from Kakao API");

        WebClient webClient = WebClient.builder()
                .baseUrl("https://kapi.kakao.com")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + kakaoAccessToken)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();

        Map<String, Object> userInfo = webClient.get()
                .uri("/v2/user/me")
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (userInfo == null) {
            log.error("[Kakao] Failed to fetch user information");
            throw new RuntimeException("카카오 사용자 정보 조회에 실패했습니다.");
        }

        log.info("[Kakao] Successfully fetched user info with id: {}", userInfo.get("id"));
        return userInfo;
    }
}
