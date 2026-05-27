package markoala.fithub.demo.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import markoala.fithub.demo.global.security.jwt.JwtProvider;
import markoala.fithub.demo.user.JobRole;
import markoala.fithub.demo.user.User;
import markoala.fithub.demo.user.UserRepository;
import markoala.fithub.demo.user.dto.OnboardingRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("User Onboarding Integration Test")
class UserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("온보딩 API: 카카오 사용자 온보딩 시 기획자(PLANNER)로 자동 설정되고 가입이 완료되는지 확인")
    void testKakaoUserOnboarding() throws Exception {
        // given: 소셜 로그인 완료 후 아직 가입이 안 된(isRegistered = false) 카카오 사용자
        User kakaoUser = User.createUser("kakaouser", "kakaouser@kakao.com", "kakao_123");
        kakaoUser.updateKakaoAccessToken("kakao_token");
        userRepository.save(kakaoUser);

        String accessToken = jwtProvider.generateAccessToken(kakaoUser);

        // 카카오 사용자는 어떤 직군을 보내더라도 PLANNER로 강제 설정됨
        OnboardingRequest request = new OnboardingRequest("kakaonick", "FRONTEND");

        // when
        mockMvc.perform(post("/api/v1/users/onboarding")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // then: DB에 기획자(PLANNER)로 설정되고 isRegistered=true가 되었는지 확인
        Optional<User> updatedUserOpt = userRepository.findById(kakaoUser.getId());
        assert updatedUserOpt.isPresent();
        User updatedUser = updatedUserOpt.get();
        assert updatedUser.isRegistered();
        assert updatedUser.getJobRole() == JobRole.PLANNER;
        assert "kakaonick".equals(updatedUser.getNickname());
    }

    @Test
    @DisplayName("온보딩 API: 깃허브 사용자 온보딩 시 개발자(BACKEND)로 설정되고 가입이 완료되는지 확인")
    void testGithubUserOnboardingSuccess() throws Exception {
        // given: 소셜 로그인 완료 후 아직 가입이 안 된(isRegistered = false) 깃허브 사용자
        User githubUser = User.createUser("githubuser", "githubuser@github.com", "github_456");
        githubUser.updateGithubAccessToken("github_token");
        userRepository.save(githubUser);

        String accessToken = jwtProvider.generateAccessToken(githubUser);

        OnboardingRequest request = new OnboardingRequest("githubnick", "BACKEND");

        // when
        mockMvc.perform(post("/api/v1/users/onboarding")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // then: DB에 설정한 개발자(BACKEND) 직군으로 설정되고 isRegistered=true가 되었는지 확인
        Optional<User> updatedUserOpt = userRepository.findById(githubUser.getId());
        assert updatedUserOpt.isPresent();
        User updatedUser = updatedUserOpt.get();
        assert updatedUser.isRegistered();
        assert updatedUser.getJobRole() == JobRole.BACKEND;
        assert "githubnick".equals(updatedUser.getNickname());
    }

    @Test
    @DisplayName("온보딩 API: 깃허브 사용자가 기획자(PLANNER)로 온보딩 시도 시 400 에러 반환 확인")
    void testGithubUserOnboardingPlannerFail() throws Exception {
        // given
        User githubUser = User.createUser("githubuser2", "githubuser2@github.com", "github_789");
        githubUser.updateGithubAccessToken("github_token2");
        userRepository.save(githubUser);

        String accessToken = jwtProvider.generateAccessToken(githubUser);

        OnboardingRequest request = new OnboardingRequest("githubnick2", "PLANNER");

        // when & then
        mockMvc.perform(post("/api/v1/users/onboarding")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("온보딩 API: 이미 온보딩을 완료한 사용자가 다시 온보딩 시도 시 400 에러 반환 확인")
    void testAlreadyRegisteredUserOnboardingFail() throws Exception {
        // given: 이미 온보딩을 완료한 사용자
        User registeredUser = User.createUser("registered", "registered@example.com", "github_999");
        registeredUser.updateGithubAccessToken("github_token_999");
        registeredUser.completeRegistration("registered@example.com", JobRole.FRONTEND);
        userRepository.save(registeredUser);

        String accessToken = jwtProvider.generateAccessToken(registeredUser);

        OnboardingRequest request = new OnboardingRequest("newnick", "FRONTEND");

        // when & then
        mockMvc.perform(post("/api/v1/users/onboarding")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}
