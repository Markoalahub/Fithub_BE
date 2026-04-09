package markoala.fithub.demo.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import markoala.fithub.demo.domain.issue.entity.Issue;
import markoala.fithub.demo.domain.issue.entity.IssueSync;
import markoala.fithub.demo.domain.issue.repository.IssueRepository;
import markoala.fithub.demo.domain.issue.repository.IssueSyncRepository;
import markoala.fithub.demo.application.service.GitHubIssueService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/issues")
@Tag(name = "Issues", description = "GitHub Issue 관리 및 동기화 API")
public class IssueController {

    private final IssueRepository issueRepository;
    private final IssueSyncRepository issueSyncRepository;
    private final GitHubIssueService gitHubIssueService;

    public IssueController(
            IssueRepository issueRepository,
            IssueSyncRepository issueSyncRepository,
            GitHubIssueService gitHubIssueService
    ) {
        this.issueRepository = issueRepository;
        this.issueSyncRepository = issueSyncRepository;
        this.gitHubIssueService = gitHubIssueService;
    }

    @GetMapping("/{issueId}")
    @Operation(summary = "Issue 조회", description = "특정 Issue의 상세 정보를 조회합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Issue 조회 성공",
                    content = @Content(schema = @Schema(implementation = Issue.class))),
            @ApiResponse(responseCode = "404", description = "Issue를 찾을 수 없음")
    })
    public ResponseEntity<Issue> getIssue(
            @Parameter(description = "Issue ID", required = true)
            @PathVariable Long issueId
    ) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new IllegalArgumentException("Issue not found: " + issueId));
        return ResponseEntity.ok(issue);
    }

    @GetMapping("/repository/{repositoryId}")
    @Operation(summary = "저장소별 Issue 목록 조회", description = "특정 저장소의 모든 Issue 목록을 조회합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Issue 목록 조회 성공"),
            @ApiResponse(responseCode = "404", description = "저장소를 찾을 수 없음")
    })
    public ResponseEntity<List<Issue>> getIssuesByRepository(
            @Parameter(description = "저장소 ID", required = true)
            @PathVariable Long repositoryId
    ) {
        List<Issue> issues = issueRepository.findByRepositoryId(repositoryId);
        return ResponseEntity.ok(issues);
    }

    @GetMapping("/sync/{issueId}")
    @Operation(summary = "Issue 동기화 상태 조회", description = "특정 Issue의 GitHub 동기화 상태를 조회합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "동기화 상태 조회 성공",
                    content = @Content(schema = @Schema(implementation = IssueSync.class))),
            @ApiResponse(responseCode = "404", description = "동기화 정보를 찾을 수 없음")
    })
    public ResponseEntity<IssueSync> getIssueSync(
            @Parameter(description = "Issue ID", required = true)
            @PathVariable Long issueId
    ) {
        IssueSync sync = issueSyncRepository.findByIssueId(issueId)
                .orElseThrow(() -> new IllegalArgumentException("IssueSync not found: " + issueId));
        return ResponseEntity.ok(sync);
    }

    @GetMapping("/sync/repository/{repositoryId}/status/{status}")
    @Operation(summary = "동기화 상태별 Issue 조회", description = "특정 동기화 상태를 가진 Issue들을 조회합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Issue 목록 조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 상태 값")
    })
    public ResponseEntity<List<IssueSync>> getIssuesByStatus(
            @Parameter(description = "저장소 ID", required = true)
            @PathVariable Long repositoryId,
            @Parameter(description = "동기화 상태 (PENDING, SYNCED, FAILED, CLOSED)", required = true)
            @PathVariable String status
    ) {
        List<IssueSync> syncs = issueSyncRepository.findByStatus(status);
        return ResponseEntity.ok(syncs);
    }

    @PatchMapping("/{issueId}/status")
    @Operation(summary = "Issue 상태 업데이트", description = "Issue의 상태를 업데이트하고 GitHub에 반영합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "상태 업데이트 성공"),
            @ApiResponse(responseCode = "400", description = "GitHub 업데이트 실패"),
            @ApiResponse(responseCode = "404", description = "Issue를 찾을 수 없음")
    })
    public ResponseEntity<Map<String, String>> updateIssueStatus(
            @Parameter(description = "Issue ID", required = true)
            @PathVariable Long issueId,
            @Parameter(description = "새로운 상태 (OPEN, CLOSED)", required = true)
            @RequestParam String status,
            @Parameter(description = "GitHub 저장소 URL", required = true)
            @RequestParam String repoUrl,
            @Parameter(description = "GitHub Access Token", required = true)
            @RequestParam String accessToken
    ) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new IllegalArgumentException("Issue not found: " + issueId));

        IssueSync sync = issueSyncRepository.findByIssueId(issueId)
                .orElseThrow(() -> new IllegalArgumentException("IssueSync not found for issue: " + issueId));

        gitHubIssueService.updateIssueStatus(sync, status, accessToken, repoUrl);
        issue.updateStatus(status);
        issueRepository.save(issue);

        return ResponseEntity.ok(Map.of(
                "issueId", issueId.toString(),
                "status", status,
                "message", "Issue status updated successfully"
        ));
    }

    @PostMapping("/{issueId}/sync")
    @Operation(summary = "Issue를 GitHub로 동기화",
            description = "Spring Issue를 GitHub 저장소에 생성하고 동기화 상태를 저장합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "동기화 성공",
                    content = @Content(schema = @Schema(implementation = IssueSync.class))),
            @ApiResponse(responseCode = "400", description = "GitHub 동기화 실패"),
            @ApiResponse(responseCode = "404", description = "Issue를 찾을 수 없음")
    })
    public ResponseEntity<IssueSync> syncIssueToGitHub(
            @Parameter(description = "Issue ID", required = true)
            @PathVariable Long issueId,
            @Parameter(description = "GitHub 저장소 URL", required = true)
            @RequestParam String repoUrl,
            @Parameter(description = "GitHub Access Token", required = true)
            @RequestParam String accessToken
    ) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new IllegalArgumentException("Issue not found: " + issueId));

        IssueSync sync = gitHubIssueService.syncIssueToGitHub(issue, repoUrl, accessToken);
        return ResponseEntity.status(HttpStatus.CREATED).body(sync);
    }
}
