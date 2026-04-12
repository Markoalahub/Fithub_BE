package markoala.fithub.demo.issue;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "repositories")
public class GithubRepository {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(nullable = false)
    private String repoUrl;

    @Column(nullable = false)
    private String repoType;

    @Column
    private String category;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public GithubRepository() {}

    public GithubRepository(Long id, Long projectId, String repoUrl, String repoType, String category, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.projectId = projectId;
        this.repoUrl = repoUrl;
        this.repoType = repoType;
        this.category = category;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public String getRepoUrl() {
        return repoUrl;
    }

    public String getRepoType() {
        return repoType;
    }

    public String getCategory() {
        return category;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public static GithubRepository createRepository(Long projectId, String repoUrl, String repoType, String category) {
        return new GithubRepository(null, projectId, repoUrl, repoType, category, null, null);
    }

    public void updateRepoType(String newRepoType) {
        this.repoType = newRepoType;
    }

    public void updateCategory(String newCategory) {
        this.category = newCategory;
    }
}
