package markoala.fithub.demo.domain.issue;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import markoala.fithub.demo.domain.github.service.GithubRepositoryService;
import markoala.fithub.demo.domain.issue.dto.GitHubRepositoryDetailResponse;
import markoala.fithub.demo.domain.issue.dto.GitHubUserRepositoriesResponse;
import markoala.fithub.demo.global.exception.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/repositories")
@Tag(name = "GitHub Repositories", description = "사용자 GitHub 레포지토리 조회 API")
public class GithubRepositoryController {

    private static final Logger log = LoggerFactory.getLogger(GithubRepositoryController.class);

    private final GithubRepositoryService githubRepositoryService;

    public GithubRepositoryController(GithubRepositoryService githubRepositoryService) {
        this.githubRepositoryService = githubRepositoryService;
    }

    @GetMapping
    @Operation(
            summary = "사용자 GitHub 레포 목록 조회",
            description = "JWT로 인증된 현재 사용자의 DB 저장 GitHub access token으로 GitHub API를 호출해 " +
                    "사용자가 접근 가능한 저장소 목록을 조회합니다. 소유, 협업자, 조직 멤버 저장소를 포함합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "GitHub 레포 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = GitHubUserRepositoriesResponse.class))),
            @ApiResponse(responseCode = "400", description = "GitHub access token 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<GitHubUserRepositoriesResponse> getAvailableRepositories(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Long userId
    ) {
        log.info("[GitHub Repository] Fetching user's GitHub repositories");

        var githubRepos = githubRepositoryService.getMyRepos(userId);

        var availableRepos = githubRepos.stream()
                .map(repo -> new GitHubUserRepositoriesResponse.AvailableGithubRepository(
                        repo.id(),
                        repo.name(),
                        repo.fullName(),
                        repo.description(),
                        repo.htmlUrl(),
                        repo.isPrivate(),
                        repo.language(),
                        repo.stargazersCount(),
                        repo.openIssuesCount()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(new GitHubUserRepositoriesResponse(availableRepos, availableRepos.size()));
    }

    @GetMapping("/{repo_id}")
    @Operation(
            summary = "GitHub 레포 단일 조회",
            description = "JWT로 인증된 현재 사용자의 DB 저장 GitHub access token으로 GitHub API를 호출해 " +
                    "repo_id에 해당하는 저장소의 기본 상세 정보를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "GitHub 레포 단일 조회 성공",
                    content = @Content(schema = @Schema(implementation = GitHubRepositoryDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "GitHub access token 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "사용자 또는 레포지토리를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "GitHub API 연결 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<GitHubRepositoryDetailResponse> getRepositoryDetail(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "GitHub 저장소 ID", example = "1028238282")
            @PathVariable("repo_id") Long repoId
    ) {
        log.info("[GitHub Repository] Fetching GitHub repository detail. repoId={}", repoId);

        var repo = githubRepositoryService.getRepoDetail(userId, repoId);

        return ResponseEntity.ok(new GitHubRepositoryDetailResponse(
                repo.id(),
                repo.name(),
                repo.fullName(),
                repo.description(),
                repo.htmlUrl(),
                repo.isPrivate(),
                repo.language(),
                repo.stargazersCount(),
                repo.openIssuesCount(),
                repo.defaultBranch(),
                repo.updatedAt(),
                repo.pushedAt(),
                repo.cloneUrl()
        ));
    }
}
