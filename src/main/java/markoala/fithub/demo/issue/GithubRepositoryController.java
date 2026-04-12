package markoala.fithub.demo.issue;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import markoala.fithub.demo.github.service.GithubRepositoryService;
import markoala.fithub.demo.issue.dto.GitHubUserRepositoriesResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/repositories")
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
            description = "인증된 사용자의 GitHub 계정에서 소유한 또는 협력자인 저장소 목록을 조회합니다. Organization 레포도 포함됩니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "GitHub 레포 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = GitHubUserRepositoriesResponse.class))),
            @ApiResponse(responseCode = "401", description = "GitHub 인증 실패")
    })
    public ResponseEntity<GitHubUserRepositoriesResponse> getAvailableRepositories() {
        log.info("[GitHub Repository] Fetching user's GitHub repositories");

        var githubRepos = githubRepositoryService.getMyRepos();

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
}
