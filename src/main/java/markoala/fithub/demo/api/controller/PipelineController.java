package markoala.fithub.demo.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import markoala.fithub.demo.application.dto.response.PipelineListResponse;
import markoala.fithub.demo.application.dto.response.PipelineResponse;
import markoala.fithub.demo.application.dto.response.PipelineStepResponse;
import markoala.fithub.demo.application.service.PipelineService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pipelines")
@Tag(name = "Pipelines", description = "AI 파이프라인 및 GitHub Issue 자동 생성 API")
public class PipelineController {

    private final PipelineService pipelineService;

    public PipelineController(PipelineService pipelineService) {
        this.pipelineService = pipelineService;
    }

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
    @Operation(summary = "파이프라인 생성 및 GitHub 동기화",
            description = "요구사항 텍스트를 기반으로 AI 파이프라인을 생성하고, Issue들을 Spring DB에 저장합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "파이프라인 생성 성공",
                    content = @Content(schema = @Schema(implementation = PipelineResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "503", description = "FastAPI 서버 연결 실패")
    })
    public ResponseEntity<PipelineResponse> generatePipeline(
            @Parameter(description = "프로젝트 ID", required = true)
            @RequestParam Long projectId,
            @Parameter(description = "개발 요구사항", required = true)
            @RequestParam String requirements,
            @Parameter(description = "카테고리 (선택)", required = false)
            @RequestParam(required = false) String category
    ) {
        PipelineResponse response = pipelineService.generatePipeline(projectId, requirements, category);
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
            @Parameter(description = "스텝 제목", required = true)
            @RequestParam String title,
            @Parameter(description = "스텝 설명", required = true)
            @RequestParam String description
    ) {
        PipelineStepResponse response = pipelineService.addStepToPipeline(pipelineId, title, description);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
            @Parameter(description = "저장소 ID", required = true)
            @RequestParam Long repositoryId,
            @Parameter(description = "GitHub Access Token", required = true)
            @RequestParam String accessToken
    ) {
        var result = pipelineService.syncPipelineToGitHub(pipelineId, repositoryId, accessToken);
        return ResponseEntity.ok(result);
    }
}
