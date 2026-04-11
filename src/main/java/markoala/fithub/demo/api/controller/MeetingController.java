package markoala.fithub.demo.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import markoala.fithub.demo.application.dto.request.MeetingConfirmStepRequest;
import markoala.fithub.demo.application.dto.response.MeetingLogResponse;
import markoala.fithub.demo.application.dto.response.MeetingStepRelationResponse;
import markoala.fithub.demo.application.dto.response.MeetingSummarizeResponse;
import markoala.fithub.demo.application.dto.response.PipelineStepResponse;
import markoala.fithub.demo.application.service.MeetingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/meetings")
@Tag(name = "Meetings", description = "회의록 생성/조회 및 파이프라인 스텝 컨펌 API")
public class MeetingController {

    private final MeetingService meetingService;

    public MeetingController(MeetingService meetingService) {
        this.meetingService = meetingService;
    }

    @PostMapping
    @Operation(
            summary = "회의록 생성",
            description = """
                    파이프라인 스텝과 연결될 회의록을 생성합니다.
                    - proposerId: 제안하는 사람 (기획자 등)
                    - recipientId: 제안 받는 사람 (개발자 등)
                    - 두 사람 모두 해당 프로젝트의 멤버여야 합니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "회의록 생성 성공",
                    content = @Content(schema = @Schema(implementation = MeetingLogResponse.class))),
            @ApiResponse(responseCode = "400", description = "프로젝트 멤버가 아닌 사용자"),
            @ApiResponse(responseCode = "404", description = "프로젝트를 찾을 수 없음")
    })
    public ResponseEntity<MeetingLogResponse> createMeeting(
            @Parameter(description = "프로젝트 ID", required = true)
            @RequestParam Long projectId,
            @Parameter(description = "회의록 내용", required = true)
            @RequestParam String content,
            @Parameter(description = "제안하는 사람 (User ID)", required = true)
            @RequestParam Long proposerId,
            @Parameter(description = "제안 받는 사람 (User ID)", required = true)
            @RequestParam Long recipientId
    ) {
        MeetingLogResponse response = meetingService.createMeeting(projectId, content, proposerId, recipientId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{meetingId}")
    @Operation(
            summary = "회의록 단건 조회",
            description = "FastAPI에서 최신 상태를 실시간으로 pull해 반환합니다 (양방향 통신 방식)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = MeetingLogResponse.class))),
            @ApiResponse(responseCode = "404", description = "회의록을 찾을 수 없음")
    })
    public ResponseEntity<MeetingLogResponse> getMeeting(
            @Parameter(description = "회의록 ID", required = true)
            @PathVariable Long meetingId
    ) {
        return ResponseEntity.ok(meetingService.getMeeting(meetingId));
    }

    @GetMapping("/project/{projectId}")
    @Operation(
            summary = "프로젝트 회의록 목록 조회",
            description = "특정 프로젝트의 모든 회의록 목록을 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public ResponseEntity<List<MeetingLogResponse>> getMeetingsByProject(
            @Parameter(description = "프로젝트 ID", required = true)
            @PathVariable Long projectId
    ) {
        return ResponseEntity.ok(meetingService.getMeetingsByProject(projectId));
    }

    @PostMapping("/{meetingId}/summarize")
    @Operation(
            summary = "회의록 AI 요약",
            description = "GPT-4o 기반으로 회의록을 요약하고 파이프라인 스텝 후보(derived_steps)를 도출합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "요약 성공",
                    content = @Content(schema = @Schema(implementation = MeetingSummarizeResponse.class))),
            @ApiResponse(responseCode = "404", description = "회의록을 찾을 수 없음")
    })
    public ResponseEntity<MeetingSummarizeResponse> summarizeMeeting(
            @Parameter(description = "회의록 ID", required = true)
            @PathVariable Long meetingId
    ) {
        return ResponseEntity.ok(meetingService.summarizeMeeting(meetingId));
    }

    @PostMapping("/{meetingId}/confirm-step")
    @Operation(
            summary = "회의록에서 파이프라인 스텝 컨펌",
            description = """
                    기획자 또는 개발자가 회의록 내용을 검토 후 파이프라인 스텝으로 확정합니다.
                    - FastAPI 파이프라인에 스텝 추가 (origin: meeting_derived)
                    - Spring DB에 Issue로 저장
                    - 회의록 ↔ 스텝 연결 관계 생성
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "스텝 컨펌 성공",
                    content = @Content(schema = @Schema(implementation = PipelineStepResponse.class))),
            @ApiResponse(responseCode = "404", description = "회의록 또는 파이프라인을 찾을 수 없음")
    })
    public ResponseEntity<PipelineStepResponse> confirmStep(
            @Parameter(description = "회의록 ID", required = true)
            @PathVariable Long meetingId,
            @Valid @RequestBody MeetingConfirmStepRequest request
    ) {
        PipelineStepResponse stepResponse = meetingService.confirmStepFromMeeting(
                meetingId, request.pipelineId(), request.title(), request.description()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(stepResponse);
    }
}
