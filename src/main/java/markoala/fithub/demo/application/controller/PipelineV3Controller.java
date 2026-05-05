package markoala.fithub.demo.application.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import markoala.fithub.demo.application.dto.response.PipelineListResponse;
import markoala.fithub.demo.application.dto.response.PipelineV3Response;
import markoala.fithub.demo.application.dto.response.PipelineStepV3Response;
import markoala.fithub.demo.application.dto.response.ProjectPipelineOverviewResponse;
import markoala.fithub.demo.application.dto.request.MeetingStepConfirmationRequest;
import markoala.fithub.demo.application.dto.request.PipelineV3Request;
import markoala.fithub.demo.application.dto.request.PipelineStepCreateRequest;
import markoala.fithub.demo.application.dto.request.PipelineStepUpdateRequest;
import markoala.fithub.demo.application.service.PipelineV3Service;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * v3 파이프라인 생성 API 컨트롤러.
 *
 * <p>FastAPI의 {@code /pipelines/generate-v3} 엔드포인트를 Spring 서버에서 프록시 호출합니다.</p>
 */
@RestController
@RequestMapping("/api/v2/pipelines")
@Tag(name = "Pipelines V3", description = "AI 파이프라인 v3 (Vertical Slice) 생성 API")
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
            @ApiResponse(responseCode = "201", description = "파이프라인 생성 성공",
                    content = @Content(schema = @Schema(implementation = PipelineV3Response.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터 (project_id 또는 requirements 누락)"),
            @ApiResponse(responseCode = "503", description = "FastAPI 서버 연결 실패")
    })
    public ResponseEntity<PipelineV3Response> generateV3Pipeline(
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
        PipelineV3Request request = new PipelineV3Request(projectId, requirements, category, techStack, file);
        PipelineV3Response response = pipelineV3Service.generateV3Pipeline(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ─────────────────────────────────────────────────────────────────
    // v3 전체 카테고리 파이프라인 일괄 생성
    // ─────────────────────────────────────────────────────────────────

    @PostMapping(value = "/generate-all", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "v3 선택 카테고리 파이프라인 일괄 생성",
            description = "전달받은 카테고리 목록(BE, FE 등)별로 v3 파이프라인을 자동 생성합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "선택한 카테고리 파이프라인 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "503", description = "FastAPI 서버 연결 실패")
    })
    public ResponseEntity<List<PipelineV3Response>> generateAllV3Pipelines(
            @Parameter(description = "프로젝트 ID", required = true)
            @RequestParam("project_id") Long projectId,

            @Parameter(description = "요구사항 텍스트", required = true)
            @RequestParam("requirements") String requirements,

            @Parameter(description = "기술 스택 (선택, 예: Spring Boot, JPA)")
            @RequestParam(value = "tech_stack", required = false) String techStack,

            @Parameter(description = "대상 카테고리 목록 (예: BE, FE)", required = true)
            @RequestParam List<String> categories,

            @Parameter(description = "PRD 파일 (선택)")
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        List<PipelineV3Response> responses = pipelineV3Service.generateV3PipelinesForCategories(
                projectId, requirements, techStack, categories, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    // ─────────────────────────────────────────────────────────────────
    // 프로젝트별 파이프라인 조회
    // ─────────────────────────────────────────────────────────────────

    @GetMapping("/{pipelineId}")
    @Operation(summary = "파이프라인 단건 조회", description = "특정 파이프라인의 모든 정보를 조회합니다.")
    public ResponseEntity<PipelineV3Response> getPipeline(
            @PathVariable Long pipelineId
    ) {
        return ResponseEntity.ok(pipelineV3Service.getPipeline(pipelineId));
    }

    @DeleteMapping("/{pipelineId}")
    @Operation(summary = "파이프라인 삭제", description = "특정 파이프라인을 완전히 삭제합니다.")
    public ResponseEntity<Void> deletePipeline(
            @PathVariable Long pipelineId
    ) {
        pipelineV3Service.deletePipeline(pipelineId);
        return ResponseEntity.noContent().build();
    }

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

    @PostMapping("/{pipelineId}/steps")
    @Operation(summary = "파이프라인 스텝 추가", description = "특정 파이프라인에 새로운 스텝을 수동으로 추가합니다.")
    public ResponseEntity<PipelineStepV3Response> addStepToPipeline(
            @PathVariable Long pipelineId,
            @RequestBody PipelineStepCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pipelineV3Service.addStepToPipeline(pipelineId, request));
    }

    @PatchMapping("/steps/{stepId}")
    @Operation(summary = "파이프라인 스텝 수정", description = "기존 파이프라인 스텝의 정보를 수정합니다.")
    public ResponseEntity<PipelineStepV3Response> updatePipelineStep(
            @PathVariable Long stepId,
            @RequestBody PipelineStepUpdateRequest request
    ) {
        return ResponseEntity.ok(pipelineV3Service.updatePipelineStep(stepId, request));
    }

    @PostMapping("/steps/{pipelineStepId}/create-issue")
    @Operation(summary = "파이프라인 스텝을 Issue로 변환", description = "사용자가 선택한 v3 파이프라인 스텝을 실제 작업 Issue로 생성하고 GitHub에 동기화합니다.")
    public ResponseEntity<markoala.fithub.demo.issue.Issue> createIssueFromStep(
            @PathVariable Long pipelineStepId,
            @RequestBody markoala.fithub.demo.pipeline.dto.CreateIssueFromStepRequest request,
            @RequestHeader(name = "Authorization") String authHeader
    ) {
        markoala.fithub.demo.issue.Issue issue = pipelineV3Service.createIssueFromPipelineStepAndSync(
                pipelineStepId,
                request.repositoryId(),
                request.title(),
                request.description(),
                request.repoUrl(),
                authHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(issue);
    }
 
    @DeleteMapping("/steps/{stepId}")
    @Operation(summary = "파이프라인 스텝 삭제", description = "특정 파이프라인 스텝을 삭제합니다.")
    public ResponseEntity<Void> deletePipelineStep(
            @PathVariable Long stepId
    ) {
        pipelineV3Service.deletePipelineStep(stepId);
        return ResponseEntity.noContent().build();
    }
    @PatchMapping("/steps/{stepId}/confirm")
    @Operation(summary = "파이프라인 스텝 승인", description = "회의 중 기획자와 개발자가 해당 스텝을 승인합니다. 승인 후에는 스텝의 내용을 자유롭게 수정할 수 있습니다.")
    public ResponseEntity<PipelineStepV3Response> confirmPipelineStep(
            @PathVariable Long stepId,
            @RequestBody MeetingStepConfirmationRequest request
    ) {
        PipelineStepUpdateRequest updateRequest = new PipelineStepUpdateRequest(
                Optional.empty(), // stepTaskDescription
                Optional.empty(), // stepDetails
                Optional.empty(), // stepSequenceNumber
                Optional.empty(), // stepGithubStatus
                Optional.of(request.plannerConfirmYn()), 
                Optional.of(request.developerConfirmYn()),
                Optional.empty(), // duration
                Optional.empty(), // techStack
                Optional.empty()  // origin
        );
        return ResponseEntity.ok(pipelineV3Service.updatePipelineStep(stepId, updateRequest));
    }

    @GetMapping("/project/{projectId}/overview")
    @Operation(summary = "프로젝트-파이프라인 통합 오버뷰 조회", description = "프로젝트의 기본 정보와 AI 파이프라인 정보를 결합한 API Composition 결과를 반환합니다.")
    public ResponseEntity<ProjectPipelineOverviewResponse> getProjectOverview(
            @PathVariable Long projectId
    ) {
        return ResponseEntity.ok(pipelineV3Service.getProjectPipelineOverview(projectId));
    }
}
