package markoala.fithub.demo.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

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
                socialLoginId
        );
        return userRepository.save(newUser);
    }

    /**
     * GitHub OAuth 사용자 정보로 사용자 조회 또는 생성
     */
    @Transactional
    public User findOrCreateGithubUser(String githubLogin, String email, Long githubId, String githubAccessToken) {
        String socialLoginId = String.valueOf(githubId);
        Optional<User> existingUser = userRepository.findBySocialLoginId(socialLoginId);

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            user.updateGithubAccessToken(githubAccessToken);
            return userRepository.save(user);
        }

        // 1. username 중복 방지 처리
        String username = githubLogin != null ? githubLogin : "github_" + socialLoginId;
        if (userRepository.findByUsername(username).isPresent()) {
            username = username + "_" + socialLoginId;
        }

        // 2. email 누락 및 중복 충돌 방지 처리
        String targetEmail = email;
        if (targetEmail == null || targetEmail.isBlank()) {
            targetEmail = "github_" + socialLoginId + "@fithub.temporary.com";
        } else if (userRepository.findByEmail(targetEmail).isPresent()) {
            targetEmail = "github_" + socialLoginId + "_" + System.currentTimeMillis() + "@fithub.temporary.com";
        }

        // 3. 새로운 GitHub 사용자 생성 (온보딩 전이므로 isRegistered = false 유지)
        User newUser = User.createUser(username, targetEmail, socialLoginId);
        newUser.updateGithubAccessToken(githubAccessToken);

        return userRepository.save(newUser);
    }

    /**
     * Kakao OAuth 사용자 정보로 사용자 조회 또는 생성
     */
    @Transactional
    public User findOrCreateKakaoUser(String nickname, String email, Long kakaoId, String kakaoAccessToken) {
        String socialLoginId = String.valueOf(kakaoId);
        Optional<User> existingUser = userRepository.findBySocialLoginId(socialLoginId);

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            user.updateKakaoAccessToken(kakaoAccessToken);
            return userRepository.save(user);
        }

        // 1. username 중복 방지 처리
        String username = nickname != null ? nickname : "kakao_" + socialLoginId;
        if (userRepository.findByUsername(username).isPresent()) {
            username = username + "_" + socialLoginId;
        }

        // 2. email 누락 및 중복 충돌 방지 처리
        String targetEmail = email;
        if (targetEmail == null || targetEmail.isBlank()) {
            targetEmail = "kakao_" + socialLoginId + "@fithub.temporary.com";
        } else if (userRepository.findByEmail(targetEmail).isPresent()) {
            targetEmail = "kakao_" + socialLoginId + "_" + System.currentTimeMillis() + "@fithub.temporary.com";
        }

        // 3. 새로운 Kakao 사용자 생성 (온보딩 전이므로 isRegistered = false 유지)
        User newUser = User.createUser(username, targetEmail, socialLoginId);
        newUser.updateKakaoAccessToken(kakaoAccessToken);

        return userRepository.save(newUser);
    }

    /**
     * 회원가입 완료: 이메일 + 직군 설정
     * - 이메일 중복 검사 (본인 제외)
     * - isRegistered = true 설정
     */
    @Transactional
    public User completeSignup(Long userId, String email, JobRole jobRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // 이미 회원가입 완료된 경우
        if (user.isRegistered()) {
            throw new IllegalStateException("이미 회원가입이 완료된 사용자입니다.");
        }

        // 이메일 중복 검사 (다른 유저가 동일 이메일 사용 중인지)
        userRepository.findByEmail(email).ifPresent(existing -> {
            if (!existing.getId().equals(userId)) {
                throw new IllegalStateException("이미 사용 중인 이메일입니다: " + email);
            }
        });

        user.completeRegistration(email, jobRole);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
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

    @Transactional
    public User updateJobRole(Long id, JobRole jobRole) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found for id: " + id));
        user.updateJobRole(jobRole);
        return userRepository.save(user);
    }

    /**
     * 회원가입 진행 (신규 유저 생성 및 연동)
     */
    @Transactional
    public User registerUser(String email, String username, JobRole jobRole, String socialLoginId, String socialType, String oauthAccessToken) {
        // 1. 이메일 중복 검사
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalStateException("이미 사용 중인 이메일입니다: " + email);
        }

        // 2. username 중복 검사
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalStateException("이미 사용 중인 이름입니다: " + username);
        }

        // 3. socialLoginId 중복 검사
        if (userRepository.findBySocialLoginId(socialLoginId).isPresent()) {
            throw new IllegalStateException("이미 가입된 소셜 계정입니다.");
        }

        // 4. User 객체 새로 빌드 및 생성 (완료된 상태로 저장)
        User newUser = User.createUser(username, email, socialLoginId);
        newUser.completeRegistration(email, jobRole);

        if ("GITHUB".equalsIgnoreCase(socialType)) {
            newUser.updateGithubAccessToken(oauthAccessToken);
        } else if ("KAKAO".equalsIgnoreCase(socialType)) {
            newUser.updateKakaoAccessToken(oauthAccessToken);
        }

        return userRepository.save(newUser);
    }

    /**
     * 온보딩 진행 (닉네임 중복 확인 및 직군 설정)
     */
    @Transactional
    public User completeOnboarding(Long userId, String nickname, String jobRoleStr) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // 1. 닉네임 중복 검사 (본인 제외)
        userRepository.findByNickname(nickname).ifPresent(existing -> {
            if (!existing.getId().equals(userId)) {
                throw new IllegalStateException("이미 사용 중인 닉네임입니다.");
            }
        });

        // 2. 닉네임 업데이트
        user.updateNickname(nickname);

        // 3. 기획자/개발자 분기 처리
        if (user.getKakaoAccessToken() != null) {
            // 카카오 로그인은 기획자로 간주
            user.updateJobRole(JobRole.PLANNER);
        } else if (user.getGithubAccessToken() != null) {
            // 깃허브 로그인은 개발자로 간주 (입력받은 직군 필수 체크 및 변환)
            JobRole parsedRole;
            try {
                if (jobRoleStr == null) {
                    throw new IllegalArgumentException();
                }
                parsedRole = JobRole.valueOf(jobRoleStr.toUpperCase());
                if (parsedRole == JobRole.PLANNER) {
                    throw new IllegalArgumentException();
                }
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "존재하지 않는 직군입니다. BACKEND, FRONTEND, AI 중에 하나를 입력해 주세요.");
            }
            user.updateJobRole(parsedRole);
        }

        // 4. 가입 완료(isRegistered) 상태 처리 (이메일은 변경하지 않음)
        user.completeRegistration(user.getEmail(), user.getJobRole());

        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public boolean isNicknameDuplicate(String nickname) {
        return userRepository.existsByNickname(nickname);
    }

    @Transactional(readOnly = true)
    public boolean isEmailDuplicate(String email) {
        return userRepository.existsByEmail(email);
    }
}
