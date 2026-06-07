package markoala.fithub.demo.domain.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Getter
@Table(name = "users")
public class User {

    public static final int DEFAULT_AI_PIPELINE_GENERATION_LIMIT = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(unique = true)
    private String nickname;

    // OAuth 제공자가 이메일을 반환하지 않을 수 있으므로 nullable=true
    // 회원가입(signup) 완료 시 반드시 설정됨
    @Column(nullable = true, unique = true)
    private String email;

    @Column(name = "social_login_id", unique = true)
    private String socialLoginId;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_role")
    private JobRole jobRole;

    // OAuth 로그인 완료 후 회원가입(이메일+직군 설정)까지 완료했는지 여부
    @Column(name = "is_registered", nullable = false, columnDefinition = "boolean default false")
    private boolean isRegistered = false;

    @Column(name = "github_access_token")
    private String githubAccessToken;

    @Column(name = "kakao_access_token")
    private String kakaoAccessToken;

    @Column(name = "ai_pipeline_generation_remaining_count", nullable = false, columnDefinition = "integer default 100")
    private Integer aiPipelineGenerationRemainingCount = DEFAULT_AI_PIPELINE_GENERATION_LIMIT;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    

    public User(Long id, String username, String nickname, String email, String socialLoginId,
                boolean isRegistered, String githubAccessToken, String kakaoAccessToken,
                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.username = username;
        this.nickname = nickname;
        this.email = email;
        this.socialLoginId = socialLoginId;
        this.isRegistered = isRegistered;
        this.githubAccessToken = githubAccessToken;
        this.kakaoAccessToken = kakaoAccessToken;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }


    public static User createUser(String username, String email, String socialLoginId) {
        return new User(null, username, null, email, socialLoginId, false, null, null, null, null);
    }

    public void updateEmail(String newEmail) {
        this.email = newEmail;
    }

    public void updateUsername(String newUsername) {
        this.username = newUsername;
    }

    public void updateNickname(String newNickname) {
        this.nickname = newNickname;
    }

    public void updateSocialLoginId(String newSocialLoginId) {
        this.socialLoginId = newSocialLoginId;
    }

    public void updateGithubAccessToken(String newGithubAccessToken) {
        this.githubAccessToken = newGithubAccessToken;
    }

    public void updateKakaoAccessToken(String newKakaoAccessToken) {
        this.kakaoAccessToken = newKakaoAccessToken;
    }

    public void updateJobRole(JobRole newJobRole) {
        this.jobRole = newJobRole;
    }

    public int getAiPipelineGenerationRemainingCount() {
        return aiPipelineGenerationRemainingCount == null
                ? DEFAULT_AI_PIPELINE_GENERATION_LIMIT
                : aiPipelineGenerationRemainingCount;
    }

    public void completeRegistration(String email, JobRole jobRole) {
        this.email = email;
        this.jobRole = jobRole;
        this.isRegistered = true;
    }
}
