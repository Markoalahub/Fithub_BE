package markoala.fithub.demo.user;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    // OAuth 제공자가 이메일을 반환하지 않을 수 있으므로 nullable=true
    // 회원가입(signup) 완료 시 반드시 설정됨
    @Column(nullable = true, unique = true)
    private String email;

    @Column(nullable = false)
    private String role;

    @Column(name = "social_login_id", unique = true)
    private String socialLoginId;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_role")
    private JobRole jobRole;

    // OAuth 로그인 완료 후 회원가입(이메일+직군 설정)까지 완료했는지 여부
    @Column(name = "is_registered", nullable = false)
    private boolean isRegistered = false;

    @Column(name = "github_access_token")
    private String githubAccessToken;

    @Column(name = "kakao_access_token")
    private String kakaoAccessToken;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public User() {}

    public User(Long id, String username, String email, String role, String socialLoginId,
                boolean isRegistered, String githubAccessToken, String kakaoAccessToken,
                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.socialLoginId = socialLoginId;
        this.isRegistered = isRegistered;
        this.githubAccessToken = githubAccessToken;
        this.kakaoAccessToken = kakaoAccessToken;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getSocialLoginId() {
        return socialLoginId;
    }

    public JobRole getJobRole() {
        return jobRole;
    }

    public boolean isRegistered() {
        return isRegistered;
    }

    public String getGithubAccessToken() {
        return githubAccessToken;
    }

    public String getKakaoAccessToken() {
        return kakaoAccessToken;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public static User createUser(String username, String email, String role, String socialLoginId) {
        return new User(null, username, email, role, socialLoginId, false, null, null, null, null);
    }

    public void updateRole(String newRole) {
        this.role = newRole;
    }

    public void updateEmail(String newEmail) {
        this.email = newEmail;
    }

    public void updateUsername(String newUsername) {
        this.username = newUsername;
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

    public void completeRegistration(String email, JobRole jobRole) {
        this.email = email;
        this.jobRole = jobRole;
        this.isRegistered = true;
    }
}
