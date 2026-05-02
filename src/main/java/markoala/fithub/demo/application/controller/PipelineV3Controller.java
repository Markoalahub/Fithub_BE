package markoala.fithub.demo.application.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import markoala.fithub.demo.application.dto.response.PipelineListResponse;
import markoala.fithub.demo.application.dto.response.PipelineResponse;
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
                    content = @Content(schema = @Schema(implementation = PipelineResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터 (project_id 또는 requirements 누락)"),
            @ApiResponse(responseCode = "503", description = "FastAPI 서버 연결 실패")
    })
    public ResponseEntity<PipelineResponse> generateV3Pipeline(
            @Parameter(description = "프로젝트 ID", required = true)
            @RequestParam("projectId") Long projectId,

            @Parameter(description = "요구사항 텍스트", required = true)
            @RequestParam("requirements") String requirements,

            @Parameter(description = "카테고리 (기본값: BE)")
            @RequestParam(value = "category", required = false, defaultValue = "BE") String category,

            @Parameter(description = "PRD 파일 (선택)")
            @RequestPart(value = "prdFile", required = false) MultipartFile prdFile
    ) {
        PipelineResponse response = pipelineV3Service.generateV3Pipeline(
                projectId, requirements, category, prdFile);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ─────────────────────────────────────────────────────────────────
    // v3 전체 카테고리 파이프라인 일괄 생성
    // ─────────────────────────────────────────────────────────────────

    @PostMapping(value = "/generate-all", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "v3 전체 카테고리 파이프라인 일괄 생성",
            description = "프로젝트에 등록된 모든 저장소의 카테고리(FE/BE/AI 등)별로 v3 파이프라인을 자동 생성합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "전체 카테고리 파이프라인 생성 성공"),
            @ApiResponse(responseCode = "400", description = "카테고리가 설정된 저장소 없음"),
            @ApiResponse(responseCode = "503", description = "FastAPI 서버 연결 실패")
    })
    public ResponseEntity<List<PipelineResponse>> generateAllV3Pipelines(
            @Parameter(description = "프로젝트 ID", required = true)
            @RequestParam Long projectId,

            @Parameter(description = "PRD 파일 (선택)")
            @RequestPart(value = "prdFile", required = false) MultipartFile prdFile
    ) {
        List<PipelineResponse> responses = pipelineV3Service.generateV3PipelinesForAllCategories(projectId, prdFile);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    // ─────────────────────────────────────────────────────────────────
    // 프로젝트별 파이프라인 조회
    // ─────────────────────────────────────────────────────────────────

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
}
