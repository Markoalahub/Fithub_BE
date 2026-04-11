package markoala.fithub.demo.domain.issue.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "issue_syncs")
public class IssueSync {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "issue_id", nullable = false)
    private Long issueId;

    @Column(name = "github_issue_number", nullable = false)
    private Integer githubIssueNumber;

    @Column(name = "repository_id", nullable = false)
    private Long repositoryId;

    @Column(nullable = false)
    private String status; // PENDING, SYNCED, FAILED, CLOSED

    @Column(name = "github_url")
    private String githubUrl;

    @Column(name = "error_message")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public IssueSync() {}

    public IssueSync(Long id, Long issueId, Integer githubIssueNumber, Long repositoryId, String status, String githubUrl, String errorMessage, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.issueId = issueId;
        this.githubIssueNumber = githubIssueNumber;
        this.repositoryId = repositoryId;
        this.status = status;
        this.githubUrl = githubUrl;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getIssueId() {
        return issueId;
    }

    public Integer getGithubIssueNumber() {
        return githubIssueNumber;
    }

    public Long getRepositoryId() {
        return repositoryId;
    }

    public String getStatus() {
        return status;
    }

    public String getGithubUrl() {
        return githubUrl;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public static IssueSync createSync(Long issueId, Integer githubIssueNumber, Long repositoryId, String status) {
        return new IssueSync(null, issueId, githubIssueNumber, repositoryId, status, null, null, null, null);
    }

    public void markSynced(String githubUrl) {
        this.status = "SYNCED";
        this.githubUrl = githubUrl;
        this.errorMessage = null;
    }

    public void markFailed(String errorMessage) {
        this.status = "FAILED";
        this.errorMessage = errorMessage;
    }

    public void markClosed() {
        this.status = "CLOSED";
    }
}
