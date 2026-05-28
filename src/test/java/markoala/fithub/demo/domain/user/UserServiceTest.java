package markoala.fithub.demo.domain.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("loadUserByUsername - 성공")
    void loadUserByUsername_Success() {
        User user = User.createUser("testuser", "test@test.com", "123");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        UserDetails details = userService.loadUserByUsername("testuser");

        assertThat(details.getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("loadUserByUsername - 실패")
    void loadUserByUsername_Fail() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.loadUserByUsername("testuser"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("findOrCreateUser - 기존 사용자")
    void findOrCreateUser_Existing() {
        User user = User.createUser("testuser", "test@test.com", "123");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        User result = userService.findOrCreateUser("testuser", "test@test.com", "123");

        assertThat(result).isEqualTo(user);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("findOrCreateUser - 새 사용자")
    void findOrCreateUser_New() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User result = userService.findOrCreateUser("testuser", "test@test.com", "123");

        assertThat(result.getUsername()).isEqualTo("testuser");
        verify(userRepository).save(any());
    }

    @Test
    @DisplayName("findOrCreateGithubUser - 기존 소셜 로그인 ID")
    void findOrCreateGithubUser_ExistingSocialId() {
        User user = User.createUser("github_123", "a@a.com", "123");
        when(userRepository.findBySocialLoginId("123")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        User result = userService.findOrCreateGithubUser("githubLogin", "a@a.com", 123L, "token");

        assertThat(result.getGithubAccessToken()).isEqualTo("token");
    }

    @Test
    @DisplayName("findOrCreateGithubUser - 새 사용자 (이름 충돌)")
    void findOrCreateGithubUser_New_NameConflict() {
        when(userRepository.findBySocialLoginId("123")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("githubLogin")).thenReturn(Optional.of(User.createUser("dummy", "dummy@test.com", "dummy_social")));
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        User result = userService.findOrCreateGithubUser("githubLogin", "test@test.com", 123L, "token");

        assertThat(result.getUsername()).isEqualTo("githubLogin_123");
        assertThat(result.getEmail()).isEqualTo("test@test.com");
    }
    
    @Test
    @DisplayName("findOrCreateGithubUser - 새 사용자 (이메일 충돌 및 null)")
    void findOrCreateGithubUser_New_EmailConflict() {
        when(userRepository.findBySocialLoginId("123")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("github_123")).thenReturn(Optional.empty());
        
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        User result = userService.findOrCreateGithubUser(null, null, 123L, "token");

        assertThat(result.getUsername()).isEqualTo("github_123");
        assertThat(result.getEmail()).isEqualTo("github_123@fithub.temporary.com");
    }

    @Test
    @DisplayName("findOrCreateKakaoUser - 기존 소셜 로그인 ID")
    void findOrCreateKakaoUser_ExistingSocialId() {
        User user = User.createUser("kakao_123", "a@a.com", "123");
        when(userRepository.findBySocialLoginId("123")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        User result = userService.findOrCreateKakaoUser("kakaoNick", "a@a.com", 123L, "token");

        assertThat(result.getKakaoAccessToken()).isEqualTo("token");
    }

    @Test
    @DisplayName("findOrCreateKakaoUser - 새 사용자 (이름 충돌)")
    void findOrCreateKakaoUser_New_NameConflict() {
        when(userRepository.findBySocialLoginId("123")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("kakaoNick")).thenReturn(Optional.of(User.createUser("dummy", "dummy@test.com", "dummy_social")));
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        User result = userService.findOrCreateKakaoUser("kakaoNick", "test@test.com", 123L, "token");

        assertThat(result.getUsername()).isEqualTo("kakaoNick_123");
    }

    @Test
    @DisplayName("completeSignup - 성공")
    void completeSignup_Success() {
        User user = new User(1L, "user1", "nick", "old@test.com", "s1", false, null, null, null, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        User result = userService.completeSignup(1L, "new@test.com", JobRole.PLANNER);

        assertThat(result.isRegistered()).isTrue();
        assertThat(result.getJobRole()).isEqualTo(JobRole.PLANNER);
    }

    @Test
    @DisplayName("completeSignup - 이미 등록됨")
    void completeSignup_AlreadyRegistered() {
        User user = new User(1L, "user1", "nick", "old@test.com", "s1", true, null, null, null, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.completeSignup(1L, "new@test.com", JobRole.PLANNER))
                .isInstanceOf(IllegalStateException.class);
    }
    
    @Test
    @DisplayName("completeSignup - 이메일 중복")
    void completeSignup_EmailDuplicate() {
        User user = new User(1L, "user1", "nick", "old@test.com", "s1", false, null, null, null, null);
        User otherUser = new User(2L, "user2", "nick2", "new@test.com", "s2", false, null, null, null, null);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.of(otherUser));

        assertThatThrownBy(() -> userService.completeSignup(1L, "new@test.com", JobRole.PLANNER))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("registerUser - 성공 (Github)")
    void registerUser_SuccessGithub() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("user1")).thenReturn(Optional.empty());
        when(userRepository.findBySocialLoginId("s1")).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        User result = userService.registerUser("test@test.com", "user1", JobRole.BACKEND, "s1", "GITHUB", "token");

        assertThat(result.getGithubAccessToken()).isEqualTo("token");
        assertThat(result.isRegistered()).isTrue();
    }
    
    @Test
    @DisplayName("registerUser - 성공 (Kakao)")
    void registerUser_SuccessKakao() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("user1")).thenReturn(Optional.empty());
        when(userRepository.findBySocialLoginId("s1")).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        User result = userService.registerUser("test@test.com", "user1", JobRole.PLANNER, "s1", "KAKAO", "token");

        assertThat(result.getKakaoAccessToken()).isEqualTo("token");
    }

    @Test
    @DisplayName("registerUser - 이메일 중복")
    void registerUser_EmailDuplicate() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(User.createUser("dummy", "dummy@test.com", "dummy_social")));
        
        assertThatThrownBy(() -> userService.registerUser("test@test.com", "user1", JobRole.BACKEND, "s1", "GITHUB", "token"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("completeOnboarding - 성공 (Kakao)")
    void completeOnboarding_SuccessKakao() {
        User user = new User(1L, "user1", null, "test@test.com", "s1", false, null, "kakao_token", null, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByNickname("nickname")).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        User result = userService.completeOnboarding(1L, "nickname", null);

        assertThat(result.getJobRole()).isEqualTo(JobRole.PLANNER);
        assertThat(result.getNickname()).isEqualTo("nickname");
        assertThat(result.isRegistered()).isTrue();
    }

    @Test
    @DisplayName("completeOnboarding - 성공 (Github - BACKEND)")
    void completeOnboarding_SuccessGithub() {
        User user = new User(1L, "user1", null, "test@test.com", "s1", false, "github_token", null, null, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByNickname("nickname")).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        User result = userService.completeOnboarding(1L, "nickname", "BACKEND");

        assertThat(result.getJobRole()).isEqualTo(JobRole.BACKEND);
    }

    @Test
    @DisplayName("completeOnboarding - 실패 (Github - 잘못된 직군)")
    void completeOnboarding_FailGithubInvalidRole() {
        User user = new User(1L, "user1", null, "test@test.com", "s1", false, "github_token", null, null, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByNickname("nickname")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.completeOnboarding(1L, "nickname", "PLANNER"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("completeOnboarding - 실패 (이미 등록됨)")
    void completeOnboarding_AlreadyRegistered() {
        User user = new User(1L, "user1", null, "test@test.com", "s1", true, "github_token", null, null, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.completeOnboarding(1L, "nickname", "BACKEND"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("completeOnboarding - 실패 (닉네임 중복)")
    void completeOnboarding_NicknameDuplicate() {
        User user = new User(1L, "user1", null, "test@test.com", "s1", false, "github_token", null, null, null);
        User otherUser = new User(2L, "user2", "nickname", "test2@test.com", "s2", false, null, null, null, null);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByNickname("nickname")).thenReturn(Optional.of(otherUser));

        assertThatThrownBy(() -> userService.completeOnboarding(1L, "nickname", "BACKEND"))
                .isInstanceOf(IllegalStateException.class);
    }
}
