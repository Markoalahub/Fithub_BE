package markoala.fithub.demo.issue.dto;

import java.util.List;

/**
 * 사용자의 GitHub 저장소 목록 응답
 * 사용자가 자신의 GitHub 계정에서 소유한 저장소들을 조회합니다
 */
public record GitHubUserRepositoriesResponse(
        List<AvailableGithubRepository> repositories,
        int totalCount
) {
    public record AvailableGithubRepository(
            Long id,
            String name,
            String fullName,
            String description,
            String htmlUrl,
            boolean isPrivate,
            String language,
            int stargazersCount,
            int openIssuesCount
    ) {}
}
