package markoala.fithub.demo.application.service;

import markoala.fithub.demo.domain.issue.entity.Issue;
import markoala.fithub.demo.domain.issue.entity.IssueSync;
import markoala.fithub.demo.domain.issue.repository.IssueSyncRepository;
import markoala.fithub.demo.global.exception.GithubApiExecutionException;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class GitHubIssueService {

    private static final Logger log = LoggerFactory.getLogger(GitHubIssueService.class);

    @Value("${github.api.token:}")
    private String githubToken;

    private final IssueSyncRepository issueSyncRepository;

    public GitHubIssueService(IssueSyncRepository issueSyncRepository) {
        this.issueSyncRepository = issueSyncRepository;
    }

    /**
     * Spring Issue를 GitHub Repository에 동기화
     * @param issue Spring Issue 엔티티
     * @param repoUrl GitHub 저장소 URL (e.g., https://github.com/owner/repo)
     * @param accessToken GitHub OAuth Access Token
     * @return IssueSync (동기화 상태)
     */
    public IssueSync syncIssueToGitHub(Issue issue, String repoUrl, String accessToken) {
        try {
            GitHub github = GitHub.connectUsingOAuth(accessToken);
            GHRepository repository = github.getRepository(extractRepoPath(repoUrl));

            // GitHub Issue 생성
            var issueBuilder = repository.createIssue(issue.getTitle());
            if (issue.getDescription() != null && !issue.getDescription().isEmpty()) {
                issueBuilder.body(issue.getDescription());
            }
            var ghIssue = issueBuilder.create();

            // 동기화 상태 저장
            IssueSync sync = IssueSync.createSync(
                    issue.getId(),
                    ghIssue.getNumber(),
                    issue.getRepositoryId(),
                    "SYNCED"
            );
            sync.markSynced(ghIssue.getHtmlUrl().toString());

            log.info("[GitHub Issue Sync] Issue #{} synced to GitHub #{} - URL: {}",
                    issue.getId(), ghIssue.getNumber(), ghIssue.getHtmlUrl());

            return issueSyncRepository.save(sync);

        } catch (IOException e) {
            log.error("[GitHub Issue Sync Error] Failed to sync Issue #{}: {}",
                    issue.getId(), e.getMessage(), e);

            IssueSync failedSync = IssueSync.createSync(
                    issue.getId(),
                    null,
                    issue.getRepositoryId(),
                    "FAILED"
            );
            failedSync.markFailed(e.getMessage());

            return issueSyncRepository.save(failedSync);
        }
    }

    /**
     * GitHub Issue 상태 업데이트
     * @param issueSync 동기화 상태
     * @param newStatus GitHub 이슈 상태 (OPEN, CLOSED)
     * @param accessToken GitHub OAuth Access Token
     * @param repoUrl GitHub 저장소 URL
     */
    public void updateIssueStatus(IssueSync issueSync, String newStatus, String accessToken, String repoUrl) {
        if (!issueSync.getStatus().equals("SYNCED")) {
            log.warn("[GitHub Issue Update] IssueSync #{} is not in SYNCED state, skipping update", issueSync.getId());
            return;
        }

        try {
            GitHub github = GitHub.connectUsingOAuth(accessToken);
            GHRepository repository = github.getRepository(extractRepoPath(repoUrl));
            var ghIssue = repository.getIssue(issueSync.getGithubIssueNumber());

            if ("CLOSED".equalsIgnoreCase(newStatus)) {
                ghIssue.close();
                issueSync.markClosed();
            } else if ("OPEN".equalsIgnoreCase(newStatus)) {
                // GitHub API에서 이슈 재개는 PR을 통해서만 가능하므로 상태만 업데이트
                log.info("[GitHub Issue Update] Issue #{} reopening not supported via API", ghIssue.getNumber());
            }

            issueSyncRepository.save(issueSync);
            log.info("[GitHub Issue Update] Issue #{} status updated to {}", issueSync.getGithubIssueNumber(), newStatus);

        } catch (IOException e) {
            log.error("[GitHub Issue Update Error] Failed to update Issue #{}: {}",
                    issueSync.getGithubIssueNumber(), e.getMessage(), e);
            throw new GithubApiExecutionException("GitHub Issue 상태 업데이트 실패: " + e.getMessage(), e, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * GitHub URL에서 owner/repo 경로 추출
     */
    private String extractRepoPath(String repoUrl) {
        // https://github.com/owner/repo → owner/repo
        return repoUrl.replaceAll("https://github.com/", "").replaceAll(".git$", "");
    }
}
