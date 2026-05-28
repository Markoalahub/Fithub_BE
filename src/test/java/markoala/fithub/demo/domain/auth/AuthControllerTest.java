package markoala.fithub.demo.domain.auth;

import markoala.fithub.demo.global.config.SecurityConfig;
import markoala.fithub.demo.global.security.jwt.JwtProvider;
import markoala.fithub.demo.domain.github.service.GithubRepositoryService;
import markoala.fithub.demo.domain.user.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}
