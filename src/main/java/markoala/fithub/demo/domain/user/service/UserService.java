package markoala.fithub.demo.domain.user.service;

import lombok.RequiredArgsConstructor;
import markoala.fithub.demo.domain.user.entity.User;
import markoala.fithub.demo.domain.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password("") // OAuth2는 비밀번호가 필요 없음
                .authorities("ROLE_USER")
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }

    /**
     * OAuth2 로그인 후 사용자를 저장하거나 조회
     */
    @Transactional
    public User findOrCreateUser(String username, String email, String socialLoginId) {
        Optional<User> existingUser = userRepository.findByUsername(username);
        
        if (existingUser.isPresent()) {
            return existingUser.get();
        }
        
        // 새로운 사용자 생성
        User newUser = User.createUser(
                username,
                email,
                socialLoginId,
                "USER"
        );
        return userRepository.save(newUser);
    }

    /**
     * GitHub OAuth 사용자 정보로 사용자 조회 또는 생성
     */
    @Transactional
    public User findOrCreateGitHubUser(String githubLogin, String email, String githubId) {
        Optional<User> existingUser = userRepository.findByUsername(githubLogin);
        
        if (existingUser.isPresent()) {
            return existingUser.get();
        }
        
        // 새로운 GitHub 사용자 생성
        User newUser = User.createUser(
                githubLogin,
                email != null ? email : githubLogin + "@github.com",
                githubId,
                "USER"
        );
        return userRepository.save(newUser);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public Optional<User> findBySocialLoginId(String socialLoginId) {
        return userRepository.findBySocialLoginId(socialLoginId);
    }

    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }
}
