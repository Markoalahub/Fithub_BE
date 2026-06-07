package markoala.fithub.demo.domain.pipeline.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;
import markoala.fithub.demo.domain.issue.Issue;
import markoala.fithub.demo.domain.pipeline.dto.response.PipelineListResponse;
import markoala.fithub.demo.domain.pipeline.dto.response.PipelineGithubRepositoryResponse;
import markoala.fithub.demo.domain.pipeline.dto.response.PipelineV3Response;
import markoala.fithub.demo.domain.pipeline.dto.response.PipelineStepV3Response;
import markoala.fithub.demo.domain.pipeline.dto.response.ProjectPipelineOverviewResponse;
import markoala.fithub.demo.domain.meeting.dto.request.MeetingStepConfirmationRequest;
import markoala.fithub.demo.domain.pipeline.dto.CreateIssueFromStepRequest;
import markoala.fithub.demo.domain.pipeline.dto.request.PipelineGithubRepositoryUpdateRequest;
import markoala.fithub.demo.domain.pipeline.dto.request.PipelineV3Request;
import markoala.fithub.demo.domain.pipeline.dto.request.PipelineStepCreateRequest;
import markoala.fithub.demo.domain.pipeline.dto.request.PipelineStepUpdateRequest;
import markoala.fithub.demo.domain.pipeline.service.PipelineV3Service;
import jakarta.validation.Valid;
import markoala.fithub.demo.global.exception.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * v3 파이프라인 생성 API 컨트롤러.
 *
 * <p>FastAPI의 {@code /pipelines/generate-v3} 엔드포인트를 Spring 서버에서 프록시 호출합니다.</p>
 */
@RestController
@RequestMapping("/pipelines")
@Tag(name = "Pipelines", description = "AI 파이프라인 생성 및 관리 API")
public class PipelineV3Controller {

    private final PipelineV3Service pipelineV3Service;

    public PipelineV3Controller(PipelineV3Service pipelineV3Service) {
        this.pipelineV3Service = pipelineV3Service;
    }

    // ─────────────────────────────────────────────────────────────────
    // v3 단일 파이프라인 생성
    // ─────────────────────────────────────────────────────────────────

    @PostMapping(value = "/generate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "v3 단일 파이프라인 생성",
            description = "요구사항 텍스트와 선택적 PRD 파일을 기반으로 AI 파이프라인을 v3 (Vertical Slice) 방식으로 생성합니다. " +
                    "내부적으로 FastAPI의 /pipelines/generate-v3 엔드포인트를 호출합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "파이프라인 생성 성공",
                    content = @Content(schema = @Schema(implementation = PipelineV3Response.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터 (project_id 누락 또는 PDF 아님)"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "프로젝트를 찾을 수 없음"),
            @ApiResponse(responseCode = "429", description = "파이프라인 생성 가능 횟수 초과"),
            @ApiResponse(responseCode = "503", description = "FastAPI 서버 연결 실패")
    })
    public ResponseEntity<?> generateV3Pipeline(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Long userId,

            @Parameter(description = "프로젝트 ID", required = true)
            @RequestParam("project_id") Long projectId,

            @Parameter(description = "요구사항 텍스트", required = true)
            @RequestParam("requirements") String requirements,

            @Parameter(description = "카테고리 (기본값: ALL)")
            @RequestParam(value = "category", required = false, defaultValue = "ALL") String category,

            @Parameter(description = "기술 스택 (선택, 예: Spring Boot, JPA)")
            @RequestParam(value = "tech_stack", required = false) String techStack,

            @Parameter(description = "PRD 파일 (선택)")
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        if (file != null && !file.isEmpty()) {
            String originalFilename = file.getOriginalFilename();
            String contentType = file.getContentType();
            if ((contentType != null && !contentType.equals("application/pdf")) &&
                (originalFilename != null && !originalFilename.toLowerCase().endsWith(".pdf"))) {
                throw new IllegalArgumentException("지원하지 않는 파일 형식입니다. PDF 파일만 업로드 가능합니다.");
            }
        }

        PipelineV3Request request = new PipelineV3Request(projectId, requirements, category, techStack, file);
        Object response = pipelineV3Service.generateV3Pipeline(userId, request);
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────────
    // 프로젝트별 파이프라인 조회
    // ─────────────────────────────────────────────────────────────────

    @Hidden
    @GetMapping("/{pipelineId}")
    @Operation(summary = "파이프라인 단건 조회", description = "특정 파이프라인의 모든 정보를 조회합니다.")
    public ResponseEntity<PipelineV3Response> getPipeline(
            @PathVariable Long pipelineId
    ) {
        return ResponseEntity.ok(pipelineV3Service.getPipeline(pipelineId));
    }

    @PatchMapping("/{pipelineId}/github")
    @Operation(
            summary = "파이프라인 GitHub repository URL 연결",
            description = "특정 파이프라인의 GitHub repository URL만 부분 수정합니다. " +
                    "하나의 GitHub repository URL은 하나의 파이프라인에만 연결됩니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "GitHub repository URL 연결 성공",
                    content = @Content(schema = @Schema(implementation = PipelineGithubRepositoryResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "파이프라인을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 다른 파이프라인에 연결된 GitHub repository URL",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PipelineGithubRepositoryResponse> updatePipelineGithubRepository(
            @Parameter(description = "GitHub repository URL을 연결할 파이프라인 ID", required = true, example = "33")
            @PathVariable Long pipelineId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "파이프라인에 연결할 GitHub repository URL",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = PipelineGithubRepositoryUpdateRequest.class),
                            examples = @ExampleObject(
                                    name = "GitHub repository URL 연결 요청",
                                    value = "{\"github_repo_url\":\"https://github.com/Markoalahub/Fithub_BE\"}"
                            )
                    )
            )
            @Valid @RequestBody PipelineGithubRepositoryUpdateRequest request
    ) {
        PipelineV3Response response = pipelineV3Service.updatePipelineGithubRepository(pipelineId, request);
        return ResponseEntity.ok(PipelineGithubRepositoryResponse.from(response));
    }

    @Hidden
    @PatchMapping("/{pipelineId}/github-repository")
    public ResponseEntity<PipelineGithubRepositoryResponse> updatePipelineGithubRepositoryLegacy(
            @PathVariable Long pipelineId,
            @Valid @RequestBody PipelineGithubRepositoryUpdateRequest request
    ) {
        return updatePipelineGithubRepository(pipelineId, request);
    }

    @Hidden
    @DeleteMapping("/{pipelineId}")
    @Operation(summary = "파이프라인 삭제", description = "특정 파이프라인을 완전히 삭제합니다.")
    public ResponseEntity<Void> deletePipeline(
            @PathVariable Long pipelineId
    ) {
        pipelineV3Service.deletePipeline(pipelineId);
        return ResponseEntity.noContent().build();
    }

    @Hidden
    @GetMapping("/project/{projectId}")
    @Operation(
            summary = "프로젝트 파이프라인 조회",
            description = "특정 프로젝트의 모든 파이프라인 목록을 v3 DTO 형식으로 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "파이프라인 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = PipelineListResponse.class))),
            @ApiResponse(responseCode = "404", description = "프로젝트를 찾을 수 없음")
    })
    public ResponseEntity<PipelineListResponse> getPipelinesByProject(
            @Parameter(description = "프로젝트 ID", required = true)
            @PathVariable Long projectId
    ) {
        PipelineListResponse response = pipelineV3Service.getPipelinesByProject(projectId);
        return ResponseEntity.ok(response);
    }
    // ─────────────────────────────────────────────────────────────────
    // 파이프라인 스텝 관리
    // ─────────────────────────────────────────────────────────────────

    @Hidden
    @PostMapping("/{pipelineId}/steps")
    @Operation(summary = "파이프라인 스텝 추가", description = "특정 파이프라인에 새로운 스텝을 수동으로 추가합니다.")
    public ResponseEntity<PipelineStepV3Response> addStepToPipeline(
            @PathVariable Long pipelineId,
            @RequestBody PipelineStepCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pipelineV3Service.addStepToPipeline(pipelineId, request));
    }

    @Hidden
    @PatchMapping("/steps/{stepId}")
    @Operation(summary = "파이프라인 스텝 수정", description = "기존 파이프라인 스텝의 정보를 수정합니다.")
    public ResponseEntity<PipelineStepV3Response> updatePipelineStep(
            @PathVariable Long stepId,
            @RequestBody PipelineStepUpdateRequest request
    ) {
        return ResponseEntity.ok(pipelineV3Service.updatePipelineStep(stepId, request));
    }

    @Hidden
    @PostMapping("/steps/{pipelineStepId}/create-issue")
    @Operation(summary = "파이프라인 스텝을 Issue로 변환", description = "사용자가 선택한 v3 파이프라인 스텝을 실제 작업 Issue로 생성하고 GitHub에 동기화합니다.")
    public ResponseEntity<Issue> createIssueFromStep(
            @PathVariable Long pipelineStepId,
            @RequestBody CreateIssueFromStepRequest request,
            @RequestHeader(name = "Authorization") String authHeader
    ) {
        Issue issue = pipelineV3Service.createIssueFromPipelineStepAndSync(
                pipelineStepId,
                request.repositoryId(),
                request.title(),
                request.description(),
                request.repoUrl(),
                authHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(issue);
    }
 
    @Hidden
    @DeleteMapping("/steps/{stepId}")
    @Operation(summary = "파이프라인 스텝 삭제", description = "특정 파이프라인 스텝을 삭제합니다.")
    public ResponseEntity<Void> deletePipelineStep(
            @PathVariable Long stepId
    ) {
        pipelineV3Service.deletePipelineStep(stepId);
        return ResponseEntity.noContent().build();
    }
    @Hidden
    @PatchMapping("/steps/{stepId}/confirm")
    @Operation(summary = "파이프라인 스텝 승인", description = "특정 회의(meetingId) 내에서 기획자와 개발자가 모두 승인했는지 확인하여 해당 스텝을 최종 승인(Approved) 처리합니다.")
    public ResponseEntity<PipelineStepV3Response> confirmPipelineStep(
            @PathVariable Long stepId,
            @RequestBody MeetingStepConfirmationRequest request
    ) {
        return ResponseEntity.ok(pipelineV3Service.confirmPipelineStep(stepId, request));
    }

    @Hidden
    @GetMapping("/project/{projectId}/overview")
    @Operation(summary = "프로젝트-파이프라인 통합 오버뷰 조회", description = "프로젝트의 기본 정보와 AI 파이프라인 정보를 결합한 API Composition 결과를 반환합니다.")
    public ResponseEntity<ProjectPipelineOverviewResponse> getProjectOverview(
            @PathVariable Long projectId
    ) {
        return ResponseEntity.ok(pipelineV3Service.getProjectPipelineOverview(projectId));
    }
}
