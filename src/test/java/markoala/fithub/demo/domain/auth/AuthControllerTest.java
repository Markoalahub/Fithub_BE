package markoala.fithub.demo.domain.auth;

import markoala.fithub.demo.global.config.SecurityConfig;
import markoala.fithub.demo.global.security.jwt.JwtProvider;
import markoala.fithub.demo.domain.github.service.GithubRepositoryService;
import markoala.fithub.demo.domain.user.UserService;
import markoala.fithub.demo.domain.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "github.client-id=mock-github-id",
        "github.client-secret=mock-github-secret",
        "github.redirect-uri=http://localhost/github/callback",
        "kakao.client-id=mock-kakao-id",
        "kakao.redirect-uri=http://localhost/kakao/callback"
})
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GithubRepositoryService githubRepositoryService;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtProvider jwtProvider;

    @MockBean
    private KakaoService kakaoService;

    @Test
    @DisplayName("GitHub 로그인 리다이렉트 성공")
    void githubLogin_Redirects() throws Exception {
        mockMvc.perform(get("/auth/login")
                        .param("frontendRedirect", "http://localhost:3000/callback")
                        .param("role", "dev-be"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrlPattern("https://github.com/login/oauth/authorize?client_id=mock-github-id&redirect_uri=http://localhost/github/callback&scope=repo,user,read:org*"));
    }

    @Test
    @DisplayName("Kakao 로그인 리다이렉트 성공")
    void kakaoLogin_Redirects() throws Exception {
        mockMvc.perform(get("/auth/kakao/login")
                        .param("frontendRedirect", "http://localhost:3000/callback")
                        .param("role", "dev-be"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrlPattern("https://kauth.kakao.com/oauth/authorize?client_id=mock-kakao-id&redirect_uri=http://localhost/kakao/callback&response_type=code*"));
    }
    @Test
    @DisplayName("GitHub 콜백 - frontendRedirect가 있으면 프론트로 fragment 토큰 리다이렉트")
    void githubCallback_RedirectsToFrontendWithFragmentTokens() throws Exception {
        User user = User.createUser("github-user", "github@test.com", "12345");

        when(githubRepositoryService.exchangeCodeForToken("mock-code")).thenReturn("github-provider-token");
        when(githubRepositoryService.getUserInfoFromGithub("github-provider-token"))
                .thenReturn(Map.of("id", 12345L, "login", "github-user", "email", "github@test.com"));
        when(userService.hasGithubAccessToken(12345L)).thenReturn(false);
        when(userService.findOrCreateGithubUser("github-user", "github@test.com", 12345L, "github-provider-token"))
                .thenReturn(user);
        when(jwtProvider.generateAccessToken(user)).thenReturn("access-token");
        when(jwtProvider.generateRefreshToken(user)).thenReturn("refresh-token");

        String state = "frontendRedirect=" + URLEncoder.encode("http://localhost:3000/auth/callback", StandardCharsets.UTF_8);

        mockMvc.perform(get("/auth/github/callback")
                        .param("code", "mock-code")
                        .param("state", state))
                .andExpect(status().isFound())
                .andExpect(header().string("Location",
                        "http://localhost:3000/auth/callback#isNew=true&accessToken=access-token&refreshToken=refresh-token"));
    }

    @Test
    @DisplayName("Kakao 콜백 - frontendRedirect가 있으면 프론트로 fragment 토큰 리다이렉트")
    void kakaoCallback_RedirectsToFrontendWithFragmentTokens() throws Exception {
        User user = User.createUser("kakao-user", "kakao@test.com", "67890");

        when(kakaoService.exchangeCodeForToken("mock-code")).thenReturn("kakao-provider-token");
        when(kakaoService.getUserInfoFromKakao("kakao-provider-token"))
                .thenReturn(Map.of(
                        "id", 67890L,
                        "properties", Map.of("nickname", "kakao-user"),
                        "kakao_account", Map.of("email", "kakao@test.com")
                ));
        when(userService.hasKakaoAccessToken(67890L)).thenReturn(false);
        when(userService.findOrCreateKakaoUser("kakao-user", "kakao@test.com", 67890L, "kakao-provider-token"))
                .thenReturn(user);
        when(jwtProvider.generateAccessToken(user)).thenReturn("access-token");
        when(jwtProvider.generateRefreshToken(user)).thenReturn("refresh-token");

        String state = "frontendRedirect=" + URLEncoder.encode("http://localhost:3000/auth/callback", StandardCharsets.UTF_8);

        mockMvc.perform(get("/auth/kakao/callback")
                        .param("code", "mock-code")
                        .param("state", state))
                .andExpect(status().isFound())
                .andExpect(header().string("Location",
                        "http://localhost:3000/auth/callback#isNew=true&accessToken=access-token&refreshToken=refresh-token"));
    }

}
