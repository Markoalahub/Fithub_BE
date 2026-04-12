package markoala.fithub.demo.issue.dto;

import markoala.fithub.demo.issue.GithubRepository;

import java.time.LocalDateTime;

public record GithubRepositoryResponse(
        Long id,
        Long projectId,
        String repoUrl,
        String repoType,
        String category,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static GithubRepositoryResponse from(GithubRepository repo) {
        return new GithubRepositoryResponse(
                repo.getId(),
                repo.getProjectId(),
                repo.getRepoUrl(),
                repo.getRepoType(),
                repo.getCategory(),
                repo.getCreatedAt(),
                repo.getUpdatedAt()
        );
    }
}
