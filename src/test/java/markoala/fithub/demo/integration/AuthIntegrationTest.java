package markoala.fithub.demo.integration;

import markoala.fithub.demo.domain.auth.KakaoService;
import markoala.fithub.demo.domain.auth.dto.SignupRequest;
import markoala.fithub.demo.domain.github.service.GithubRepositoryService;
import markoala.fithub.demo.global.security.jwt.JwtProvider;
import markoala.fithub.demo.domain.user.JobRole;
import markoala.fithub.demo.domain.user.User;
import markoala.fithub.demo.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Authentication Integration Test")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GithubRepositoryService githubRepositoryService;

    @MockBean
    private KakaoService kakaoService;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("회원가입 API: 신규 회원이 가입에 성공하여 JWT를 발급받는지 확인")
    void testSignupSuccess() throws Exception {
        SignupRequest request = new SignupRequest(
                "newuser@example.com",
                "newuser",
                JobRole.PLANNER,
                "github_12345",
                "GITHUB",
                "ghp_mock_token_123"
        );

        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("회원가입이 성공적으로 완료되었습니다."))
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());

        // DB에 실제로 저장되었는지 확인
        System.out.println("=== Debug: findAll ===");
        userRepository.findAll().forEach(u -> System.out.println("User in DB: id=" + u.getId() + ", email=" + u.getEmail() + ", username=" + u.getUsername() + ", isRegistered=" + u.isRegistered() + ", jobRole=" + u.getJobRole()));
        
        Optional<User> savedUserOpt = userRepository.findByEmail("newuser@example.com");
        if (!savedUserOpt.isPresent()) {
            throw new AssertionError("User not found by findByEmail in DB! Email: newuser@example.com");
        }
        User savedUser = savedUserOpt.get();
        if (!"newuser".equals(savedUser.getUsername())) throw new AssertionError("Username mismatch");
        if (JobRole.PLANNER != savedUser.getJobRole()) throw new AssertionError("JobRole mismatch");
        if (!"github_12345".equals(savedUser.getSocialLoginId())) throw new AssertionError("SocialLoginId mismatch");
        if (!"ghp_mock_token_123".equals(savedUser.getGithubAccessToken())) throw new AssertionError("GithubAccessToken mismatch");
    }

    @Test
    @DisplayName("회원가입 API: 이미 존재하는 이메일로 가입 시 409 Conflict 발생 확인")
    void testSignupDuplicateEmail() throws Exception {
        // 이미 가입된 회원 사전 저장
        User existingUser = User.createUser("existing", "existing@example.com", "github_999");
        existingUser.updateGithubAccessToken("ghp_some");
        existingUser.completeRegistration("existing@example.com", JobRole.BACKEND);
        userRepository.save(existingUser);

        SignupRequest request = new SignupRequest(
                "existing@example.com", // 중복 이메일
                "newuser",
                JobRole.BACKEND,
                "github_12345",
                "GITHUB",
                "ghp_mock_token_123"
        );

        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("이미 사용 중인 이메일입니다: existing@example.com"));
    }

    @Test
    @DisplayName("GitHub 콜백 API: 신규 소셜 유저 로그인 시, 즉시 자동 회원가입이 완료되고 JWT가 발급되는지 확인")
    void testGithubCallbackNewUser() throws Exception {
        // Mocking GitHub services
        when(githubRepositoryService.exchangeCodeForToken("mock_code")).thenReturn("ghp_mock_token");
        
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", 98765L);
        userInfo.put("login", "github_coder");
        userInfo.put("email", "coder@github.com");
        when(githubRepositoryService.getUserInfoFromGithub("ghp_mock_token")).thenReturn(userInfo);

        mockMvc.perform(get("/auth/github/callback")
                .param("code", "mock_code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isNew").value(true))
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());

        // DB에 즉시 자동 가입(isRegistered=true) 상태로 영속화되었는지 검증
        userRepository.findAll(); // H2 EntityManager Flush 유도
        Optional<User> savedUserOpt = userRepository.findBySocialLoginId("98765");
        if (!savedUserOpt.isPresent()) {
            throw new AssertionError("User not found by findBySocialLoginId in DB! SocialLoginId: 98765");
        }
        User savedUser = savedUserOpt.get();
        if (savedUser.isRegistered()) {
            throw new AssertionError("User registration should be incomplete (isRegistered = false)");
        }
    }

    @Test
    @DisplayName("GitHub 콜백 API: 기존 소셜 유저 로그인 시, JWT 발급 및 로그인 처리가 완료되고 user.id가 반환되는지 확인")
    void testGithubCallbackExistingUser() throws Exception {
        // 기존 소셜 유저 DB 저장
        User existingUser = User.createUser("github_coder", "coder@github.com", "98765");
        existingUser.updateGithubAccessToken("old_token");
        existingUser.completeRegistration("coder@github.com", JobRole.BACKEND);
        userRepository.save(existingUser);

        when(githubRepositoryService.exchangeCodeForToken("mock_code")).thenReturn("ghp_new_token");
        
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", 98765L);
        userInfo.put("login", "github_coder");
        userInfo.put("email", "coder@github.com");
        when(githubRepositoryService.getUserInfoFromGithub("ghp_new_token")).thenReturn(userInfo);

        mockMvc.perform(get("/auth/github/callback")
                .param("code", "mock_code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isNew").value(false))
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());

        // access token이 정상 갱신되었는지 확인
        userRepository.findAll(); // Flush 유도
        Optional<User> updatedUserOpt = userRepository.findBySocialLoginId("98765");
        assert updatedUserOpt.isPresent();
        assert updatedUserOpt.get().getGithubAccessToken().equals("ghp_new_token");
    }

    @Test
    @DisplayName("Kakao 콜백 API: 신규 소셜 유저 로그인 시, 즉시 자동 회원가입이 완료되고 JWT가 발급되는지 확인")
    void testKakaoCallbackNewUser() throws Exception {
        when(kakaoService.exchangeCodeForToken("mock_code")).thenReturn("kakao_mock_token");

        Map<String, Object> properties = new HashMap<>();
        properties.put("nickname", "kakaouser");

        Map<String, Object> kakaoAccount = new HashMap<>();
        kakaoAccount.put("email", "kakaouser@kakao.com");

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", 123456L);
        userInfo.put("properties", properties);
        userInfo.put("kakao_account", kakaoAccount);

        when(kakaoService.getUserInfoFromKakao("kakao_mock_token")).thenReturn(userInfo);

        mockMvc.perform(get("/auth/kakao/callback")
                .param("code", "mock_code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isNew").value(true))
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());

        // DB에 즉시 자동 가입(isRegistered=true) 상태로 영속화되었는지 검증
        userRepository.findAll(); // H2 EntityManager Flush 유도
        Optional<User> savedUserOpt = userRepository.findBySocialLoginId("123456");
        if (!savedUserOpt.isPresent()) {
            throw new AssertionError("User not found by findBySocialLoginId in DB! SocialLoginId: 123456");
        }
        User savedUser = savedUserOpt.get();
        if (savedUser.isRegistered()) {
            throw new AssertionError("User registration should be incomplete (isRegistered = false)");
        }
    }

    @Test
    @DisplayName("Kakao 콜백 API: 기존 소셜 유저 로그인 시, JWT 발급 및 로그인 처리가 완료되고 user.id가 반환되는지 확인")
    void testKakaoCallbackExistingUser() throws Exception {
        // 기존 소셜 유저 DB 저장
        User existingUser = User.createUser("kakaouser", "kakaouser@kakao.com", "123456");
        existingUser.updateKakaoAccessToken("old_kakao_token");
        existingUser.completeRegistration("kakaouser@kakao.com", JobRole.BACKEND);
        userRepository.save(existingUser);

        when(kakaoService.exchangeCodeForToken("mock_code")).thenReturn("kakao_new_token");

        Map<String, Object> properties = new HashMap<>();
        properties.put("nickname", "kakaouser");

        Map<String, Object> kakaoAccount = new HashMap<>();
        kakaoAccount.put("email", "kakaouser@kakao.com");

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", 123456L);
        userInfo.put("properties", properties);
        userInfo.put("kakao_account", kakaoAccount);

        when(kakaoService.getUserInfoFromKakao("kakao_new_token")).thenReturn(userInfo);

        mockMvc.perform(get("/auth/kakao/callback")
                .param("code", "mock_code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isNew").value(false))
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());

        // access token이 정상 갱신되었는지 확인
        userRepository.findAll(); // Flush 유도
        Optional<User> updatedUserOpt = userRepository.findBySocialLoginId("123456");
        assert updatedUserOpt.isPresent();
        assert updatedUserOpt.get().getKakaoAccessToken().equals("kakao_new_token");
    }
}
