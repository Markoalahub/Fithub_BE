package markoala.fithub.demo.pipeline;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import markoala.fithub.demo.issue.Issue;
import markoala.fithub.demo.pipeline.dto.CreateIssueFromStepRequest;
import markoala.fithub.demo.pipeline.dto.MultiPipelineResponse;
import markoala.fithub.demo.pipeline.dto.PipelineGenerateRequest;
import markoala.fithub.demo.pipeline.dto.PipelineListResponse;
import markoala.fithub.demo.pipeline.dto.PipelineResponse;
import markoala.fithub.demo.pipeline.dto.PipelineStepAddRequest;
import markoala.fithub.demo.pipeline.dto.PipelineStepResponse;
import markoala.fithub.demo.pipeline.dto.PipelineStepUpdateRequest;
import markoala.fithub.demo.pipeline.dto.SyncPipelineToGitHubRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/pipelines")
@Tag(name = "Pipelines", description = "AI 파이프라인 및 GitHub Issue 자동 생성 API")
public class PipelineController {

    private final PipelineService pipelineService;

    public PipelineController(PipelineService pipelineService) {
        this.pipelineService = pipelineService;
    }

    // ─────────────────────────────────────────────────────────────────
    // 핵심: 프로젝트 내 모든 카테고리 파이프라인 일괄 생성 (PDF PRD 지원)
    // ─────────────────────────────────────────────────────────────────
    @PostMapping(value = "/generate-all", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "전체 카테고리 파이프라인 생성 (PDF PRD 기반)",
        description = "프로젝트에 등록된 모든 저장소의 카테고리(FE/BE/AI 등)별로 파이프라인을 생성합니다. " +
                      "PDF 형태의 PRD 파일을 업로드하면 AI가 분석하여 직군별 파이프라인을 자동 생성합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "전체 카테고리 파이프라인 생성 성공",
                content = @Content(schema = @Schema(implementation = MultiPipelineResponse.class))),
        @ApiResponse(responseCode = "400", description = "카테고리가 설정된 저장소 없음"),
        @ApiResponse(responseCode = "503", description = "FastAPI 서버 연결 실패")
    })
    public ResponseEntity<MultiPipelineResponse> generateAllCategoryPipelines(
            @Parameter(description = "프로젝트 ID", required = true)
            @RequestParam Long projectId,
            @Parameter(description = "PRD PDF 파일 (선택 - 없으면 요구사항 없이 생성)")
            @RequestPart(value = "prdFile", required = false) MultipartFile prdFile
    ) throws IOException {
        byte[] pdfBytes = (prdFile != null && !prdFile.isEmpty()) ? prdFile.getBytes() : null;
        MultiPipelineResponse response = pipelineService.generatePipelinesForAllCategories(projectId, pdfBytes);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/project/{projectId}")
    @Operation(summary = "프로젝트 파이프라인 조회", description = "특정 프로젝트의 모든 파이프라인 목록을 조회합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "파이프라인 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = PipelineListResponse.class))),
            @ApiResponse(responseCode = "404", description = "프로젝트를 찾을 수 없음")
    })
    public ResponseEntity<PipelineListResponse> getPipelinesByProject(
            @Parameter(description = "Spring Project ID", required = true)
            @PathVariable Long projectId
    ) {
        PipelineListResponse response = pipelineService.getPipelinesByProject(projectId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/generate")
    @Operation(summary = "단일 파이프라인 생성",
            description = "요구사항 텍스트를 기반으로 특정 카테고리의 AI 파이프라인을 생성합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "파이프라인 생성 성공",
                    content = @Content(schema = @Schema(implementation = PipelineResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "503", description = "FastAPI 서버 연결 실패")
    })
    public ResponseEntity<PipelineResponse> generatePipeline(
            @Valid @RequestBody PipelineGenerateRequest request
    ) {
        PipelineResponse response = pipelineService.generatePipeline(
                request.projectId(), request.requirements(), request.category());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{pipelineId}/steps")
    @Operation(summary = "파이프라인에 스텝 추가",
            description = "기존 파이프라인에 새로운 스텝(Issue)을 수동으로 추가합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "스텝 추가 성공",
                    content = @Content(schema = @Schema(implementation = PipelineStepResponse.class))),
            @ApiResponse(responseCode = "404", description = "파이프라인을 찾을 수 없음")
    })
    public ResponseEntity<PipelineStepResponse> addStepToPipeline(
            @Parameter(description = "파이프라인 ID", required = true)
            @PathVariable Long pipelineId,
            @Valid @RequestBody PipelineStepAddRequest request
    ) {
        PipelineStepResponse response = pipelineService.addStepToPipeline(
                pipelineId, request.title(), request.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ─────────────────────────────────────────────────────────────────
    // 파이프라인 스텝 → Issue 변환 (사용자 선택)
    // ─────────────────────────────────────────────────────────────────
    @PostMapping("/steps/{pipelineStepId}/create-issue")
    @Operation(summary = "파이프라인 스텝을 Issue로 변환",
            description = "사용자가 선택한 파이프라인 스텝을 실제 작업 Issue로 생성합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Issue 생성 성공",
                    content = @Content(schema = @Schema(implementation = Issue.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "404", description = "저장소를 찾을 수 없음")
    })
    public ResponseEntity<Issue> createIssueFromStep(
            @Parameter(description = "FastAPI Pipeline Step ID", required = true)
            @PathVariable Long pipelineStepId,
            @Valid @RequestBody CreateIssueFromStepRequest request
    ) {
        Issue issue = pipelineService.createIssueFromPipelineStep(
                pipelineStepId, request.repositoryId(), request.title(), request.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(issue);
    }

    @PutMapping("/steps/{stepId}")
    @Operation(summary = "파이프라인 스텝 수정",
            description = "파이프라인 스텝의 제목, 설명, 완료 여부를 수정합니다. 개발자/기획자가 AI 생성 스텝을 조정할 수 있습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "스텝 수정 성공",
                    content = @Content(schema = @Schema(implementation = PipelineStepResponse.class))),
            @ApiResponse(responseCode = "404", description = "스텝을 찾을 수 없음")
    })
    public ResponseEntity<PipelineStepResponse> updateStep(
            @Parameter(description = "파이프라인 스텝 ID", required = true)
            @PathVariable Long stepId,
            @Valid @RequestBody PipelineStepUpdateRequest request
    ) {
        PipelineStepResponse response = pipelineService.updatePipelineStep(stepId, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/steps/{stepId}/complete")
    @Operation(summary = "파이프라인 스텝 완료 처리",
            description = "특정 파이프라인 스텝을 완료 상태로 변경합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "스텝 완료 처리 성공"),
            @ApiResponse(responseCode = "404", description = "스텝을 찾을 수 없음")
    })
    public ResponseEntity<Void> completeStep(
            @Parameter(description = "파이프라인 스텝 ID", required = true)
            @PathVariable Long stepId
    ) {
        pipelineService.completeStep(stepId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{pipelineId}/sync-to-github")
    @Operation(summary = "파이프라인을 GitHub Issues로 동기화",
            description = "파이프라인의 모든 스텝을 GitHub Issues로 생성하고 상태를 추적합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "동기화 성공"),
            @ApiResponse(responseCode = "400", description = "GitHub 동기화 실패"),
            @ApiResponse(responseCode = "401", description = "GitHub 인증 실패")
    })
    public ResponseEntity<?> syncPipelineToGitHub(
            @Parameter(description = "파이프라인 ID", required = true)
            @PathVariable Long pipelineId,
            @Valid @RequestBody SyncPipelineToGitHubRequest request
    ) {
        var result = pipelineService.syncPipelineToGitHub(pipelineId, request.repositoryId(), request.accessToken());
        return ResponseEntity.ok(result);
    }
}
