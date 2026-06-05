package markoala.fithub.demo.domain.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import markoala.fithub.demo.global.config.SecurityConfig;
import markoala.fithub.demo.global.security.jwt.JwtProvider;
import markoala.fithub.demo.domain.user.dto.OnboardingRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        when(jwtProvider.validateToken(anyString())).thenReturn(true);
        when(jwtProvider.getUserIdFromToken(anyString())).thenReturn(1L);
    }

    @Test
    @DisplayName("온보딩 성공")
    void onboardUser_Success() throws Exception {
        OnboardingRequest request = new OnboardingRequest("newNickname", "PLANNER");

        when(userService.completeOnboarding(eq(1L), eq("newNickname"), eq("PLANNER"))).thenReturn(User.createUser("dummy", "dummy@test.com", "dummy_social"));

        mockMvc.perform(post("/users/onboarding")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("온보딩 실패 - 닉네임 누락 (400)")
    void onboardUser_ValidationFail() throws Exception {
        OnboardingRequest request = new OnboardingRequest("", "PLANNER");

        mockMvc.perform(post("/users/onboarding")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("닉네임 중복 체크 성공 - 중복 안 됨")
    void checkNicknameDuplicate_NotDuplicate() throws Exception {
        when(userService.isNicknameDuplicate("newNick")).thenReturn(false);

        mockMvc.perform(get("/users/check-nickname")
                        .header("Authorization", "Bearer token")
                        .param("nickname", "newNick"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isDuplicate").value(false));
    }

    @Test
    @DisplayName("닉네임 중복 체크 성공 - 중복됨")
    void checkNicknameDuplicate_Duplicate() throws Exception {
        when(userService.isNicknameDuplicate("newNick")).thenReturn(true);

        mockMvc.perform(get("/users/check-nickname")
                        .header("Authorization", "Bearer token")
                        .param("nickname", "newNick"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isDuplicate").value(true));
    }

    @Test
    @DisplayName("닉네임으로 사용자 조회 성공")
    void getUserByNickname_Success() throws Exception {
        User mockUser = User.createUser("dummy", "dummy@test.com", "dummy_social");
        // Since User id is private without setter, we can just return a non-null User, UserResponse mapping handles it.
        // Even if empty, mapping to UserResponse should succeed.
        when(userService.findByNickname("userNick")).thenReturn(Optional.of(mockUser));

        mockMvc.perform(get("/users")
                        .header("Authorization", "Bearer token")
                        .param("nickname", "userNick"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("닉네임으로 사용자 조회 실패 - 사용자 없음 (404)")
    void getUserByNickname_NotFound() throws Exception {
        when(userService.findByNickname("userNick")).thenReturn(Optional.empty());

        mockMvc.perform(get("/users")
                        .header("Authorization", "Bearer token")
                        .param("nickname", "userNick"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("현재 로그인 사용자 조회 성공")
    void getCurrentUser_Success() throws Exception {
        User mockUser = new User(1L, "githubUser", "fitdev", "fitdev@test.com",
                "social_1", true, null, null, null, null);
        mockUser.updateJobRole(JobRole.BACKEND);

        when(userService.findById(1L)).thenReturn(Optional.of(mockUser));

        mockMvc.perform(get("/users/me")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_id").value(1L))
                .andExpect(jsonPath("$.nickname").value("fitdev"))
                .andExpect(jsonPath("$.job_role").value("BACKEND"));

        verify(userService).findById(1L);
    }

    @Test
    @DisplayName("현재 로그인 사용자 조회 실패 - 사용자 없음 (404)")
    void getCurrentUser_NotFound() throws Exception {
        when(userService.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/users/me")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다: 1"));

        verify(userService).findById(1L);
    }
}
