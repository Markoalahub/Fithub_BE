package markoala.fithub.demo.domain.issue.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "issues")
public class Issue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "repository_id", nullable = false)
    private Long repositoryId;

    @Column(nullable = false)
    private Integer githubIssueNumber;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String status;

    @Column(name = "pipeline_step_id")
    private Integer pipelineStepId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Issue() {}

    public Issue(Long id, Long repositoryId, Integer githubIssueNumber, String title, String description, String status, Integer pipelineStepId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.repositoryId = repositoryId;
        this.githubIssueNumber = githubIssueNumber;
        this.title = title;
        this.description = description;
        this.status = status;
        this.pipelineStepId = pipelineStepId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getRepositoryId() {
        return repositoryId;
    }

    public Integer getGithubIssueNumber() {
        return githubIssueNumber;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public Integer getPipelineStepId() {
        return pipelineStepId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public static Issue createIssue(Long repositoryId, Integer githubIssueNumber, String title, String description, String status) {
        return new Issue(null, repositoryId, githubIssueNumber, title, description, status, null, null, null);
    }

    public void updateStatus(String newStatus) {
        this.status = newStatus;
    }

    public void setPipelineStepId(Integer pipelineStepId) {
        this.pipelineStepId = pipelineStepId;
    }

    public void updateTitle(String newTitle) {
        this.title = newTitle;
    }

    public void updateDescription(String newDescription) {
        this.description = newDescription;
    }
}
