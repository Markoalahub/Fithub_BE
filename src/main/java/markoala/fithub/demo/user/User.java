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

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String role;

    @Column(name = "social_login_id", unique = true)
    private String socialLoginId;

    @Column(name = "github_access_token")
    private String githubAccessToken;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public User() {}

    public User(Long id, String username, String email, String role, String socialLoginId, String githubAccessToken, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.socialLoginId = socialLoginId;
        this.githubAccessToken = githubAccessToken;
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

    public String getGithubAccessToken() {
        return githubAccessToken;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public static User createUser(String username, String email, String role, String socialLoginId) {
        return new User(null, username, email, role, socialLoginId, null, null, null);
    }

    public void updateRole(String newRole) {
        this.role = newRole;
    }

    public void updateSocialLoginId(String newSocialLoginId) {
        this.socialLoginId = newSocialLoginId;
    }

    public void updateGithubAccessToken(String newGithubAccessToken) {
        this.githubAccessToken = newGithubAccessToken;
    }
}
