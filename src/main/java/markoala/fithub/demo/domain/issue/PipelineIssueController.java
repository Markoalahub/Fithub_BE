package markoala.fithub.demo.domain.issue;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import markoala.fithub.demo.domain.issue.dto.RepositoryIssueCreateRequest;
import markoala.fithub.demo.domain.issue.dto.RepositoryIssueCreateResponse;
import markoala.fithub.demo.global.exception.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pipelines")
@Tag(name = "Pipelines", description = "AI 파이프라인 생성 및 관리 API")
public class PipelineIssueController {

    private static final Logger log = LoggerFactory.getLogger(PipelineIssueController.class);

    private final RepositoryIssueService repositoryIssueService;

    public PipelineIssueController(RepositoryIssueService repositoryIssueService) {
        this.repositoryIssueService = repositoryIssueService;
    }

    @PostMapping("/{pipelineId}/issues")
    @Operation(
            summary = "파이프라인 연결 레포지토리에 GitHub 이슈 생성",
            description = "pipelineId로 파이프라인을 조회하고, 해당 파이프라인의 github_repo_url에 연결된 GitHub 레포지토리에 " +
                    "feat_detail 값을 제목으로 사용해 GitHub 이슈를 생성합니다. body 값은 GitHub 이슈 본문으로 저장됩니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "GitHub 이슈 생성 성공",
                    content = @Content(schema = @Schema(implementation = RepositoryIssueCreateResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 또는 파이프라인 GitHub repository URL 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "사용자, 레포지토리 또는 파이프라인을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "GitHub API 연결 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RepositoryIssueCreateResponse> createPipelineIssue(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "파이프라인 ID", example = "37")
            @PathVariable Long pipelineId,
            @Valid @RequestBody RepositoryIssueCreateRequest request
    ) {
        log.info("[Pipeline Issue] Creating GitHub issue. pipelineId={}", pipelineId);

        RepositoryIssueCreateResponse response = repositoryIssueService.createIssue(userId, pipelineId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
