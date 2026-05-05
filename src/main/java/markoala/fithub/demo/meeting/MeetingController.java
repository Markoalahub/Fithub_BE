package markoala.fithub.demo.meeting;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import markoala.fithub.demo.meeting.dto.MeetingConfirmStepRequest;
import markoala.fithub.demo.meeting.dto.MeetingCreateRequest;
import markoala.fithub.demo.meeting.dto.MeetingLogResponse;
import markoala.fithub.demo.meeting.dto.MeetingStepRelationResponse;
import markoala.fithub.demo.meeting.dto.MeetingSummarizeResponse;
import markoala.fithub.demo.pipeline.dto.PipelineStepResponse;
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
            @Valid @RequestBody MeetingCreateRequest request
    ) {
        MeetingLogResponse response = meetingService.createMeeting(
                request.projectId(), request.content(), request.proposerId(), request.recipientId());
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

    @GetMapping("/search")
    @Operation(summary = "번역 세션 임베딩 검색", description = "과거 회의 및 번역 세션 내용을 임베딩 기반으로 의미론적 검색을 수행합니다.")
    public ResponseEntity<markoala.fithub.demo.meeting.dto.TranslationSearchResponse> searchMeetings(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(meetingService.searchMeetings(query, limit));
    }

    @PostMapping("/{meetingId}/translate-to-technical")
    @Operation(summary = "기획자 -> 개발자 기술 번역", description = "기획자의 요구사항을 개발자가 이해할 수 있는 기술 용어와 구현 방식으로 번역합니다.")
    public ResponseEntity<markoala.fithub.demo.meeting.dto.TranslateToTechnicalResponse> translateToTechnical(
            @PathVariable Long meetingId,
            @RequestBody markoala.fithub.demo.meeting.dto.TranslateToTechnicalRequest request
    ) {
        return ResponseEntity.ok(meetingService.translateToTechnical(meetingId, request));
    }

    @PostMapping("/{meetingId}/translate-to-planning")
    @Operation(summary = "개발자 -> 기획자 비즈니스 번역", description = "개발자의 기술적 설명을 기획자가 이해할 수 있는 비즈니스 가치와 비유로 번역합니다.")
    public ResponseEntity<markoala.fithub.demo.meeting.dto.TranslateToPlanningResponse> translateToPlanning(
            @PathVariable Long meetingId,
            @RequestBody markoala.fithub.demo.meeting.dto.TranslateToPlanningRequest request
    ) {
        return ResponseEntity.ok(meetingService.translateToPlanning(meetingId, request));
    }

    @PostMapping("/{meetingId}/finalize")
    @Operation(summary = "번역 세션 종료", description = "현재 번역 대화를 종료하고 전체 내용을 요약하여 검색 인덱스에 저장합니다.")
    public ResponseEntity<Void> finalizeSession(@PathVariable Long meetingId) {
        meetingService.finalizeTranslationSession(meetingId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{meetingId}/translation-history")
    @Operation(summary = "번역 이력 조회", description = "특정 회의 세션의 전체 번역 대화 이력을 조회합니다.")
    public ResponseEntity<Object> getTranslationHistory(@PathVariable Long meetingId) {
        return ResponseEntity.ok(meetingService.getTranslationHistory(meetingId));
    }

    @PatchMapping("/{meetingId}")
    @Operation(summary = "회의록 내용 수정", description = "생성된 회의록의 텍스트 내용을 수정합니다.")
    public ResponseEntity<MeetingLogResponse> updateMeeting(
            @PathVariable Long meetingId,
            @RequestBody String content
    ) {
        return ResponseEntity.ok(meetingService.updateMeeting(meetingId, content));
    }

    @DeleteMapping("/{meetingId}")
    @Operation(summary = "회의록 삭제", description = "특정 회의록을 삭제합니다.")
    public ResponseEntity<Void> deleteMeeting(@PathVariable Long meetingId) {
        meetingService.deleteMeeting(meetingId);
        return ResponseEntity.noContent().build();
    }
}
