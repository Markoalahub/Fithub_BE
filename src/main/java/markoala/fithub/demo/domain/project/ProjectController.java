package markoala.fithub.demo.domain.project;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import markoala.fithub.demo.domain.project.dto.ProjectCreateRequest;
import markoala.fithub.demo.domain.project.dto.ProjectInviteRequest;
import markoala.fithub.demo.domain.project.dto.ProjectInviteResponse;
import markoala.fithub.demo.domain.project.dto.ProjectMemberAddRequest;
import markoala.fithub.demo.domain.project.dto.ProjectMemberRoleUpdateRequest;
import markoala.fithub.demo.domain.project.dto.ProjectUpdateRequest;
import markoala.fithub.demo.domain.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
@Tag(name = "Projects", description = "프로젝트 관리 및 멤버 관리 API")
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final markoala.fithub.demo.domain.pipeline.service.PipelineV3Service pipelineV3Service;

    public ProjectController(
            ProjectService projectService,
            ProjectMemberRepository projectMemberRepository,
            UserRepository userRepository,
            markoala.fithub.demo.domain.pipeline.service.PipelineV3Service pipelineV3Service
    ) {
        this.projectService = projectService;
        this.projectMemberRepository = projectMemberRepository;
        this.userRepository = userRepository;
        this.pipelineV3Service = pipelineV3Service;
    }

    @GetMapping("/me")
    @Operation(summary = "내 프로젝트 목록 조회", description = "JWT 토큰을 기반으로 내가 참여 중인 프로젝트 목록을 조회합니다")
    @ApiResponse(responseCode = "200", description = "프로젝트 목록 조회 성공")
    public ResponseEntity<List<Project>> getMyProjects(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(projectService.getUserProjects(userId));
    }

    @Hidden
    @GetMapping("/{projectId}")
    @Operation(summary = "프로젝트 조회", description = "특정 프로젝트의 상세 정보를 조회합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "프로젝트 조회 성공",
                    content = @Content(schema = @Schema(implementation = Project.class))),
            @ApiResponse(responseCode = "404", description = "프로젝트를 찾을 수 없음")
    })
    public ResponseEntity<Project> getProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(projectService.getProject(projectId));
    }

    @PostMapping
    @Operation(summary = "프로젝트 생성", description = "새로운 프로젝트를 생성합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "프로젝트 생성 성공",
                    content = @Content(schema = @Schema(implementation = markoala.fithub.demo.domain.project.dto.ProjectCreateResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터 또는 이름 중복")
    })
    public ResponseEntity<markoala.fithub.demo.domain.project.dto.ProjectCreateResponse> createProject(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ProjectCreateRequest request
    ) {
        markoala.fithub.demo.domain.project.dto.ProjectCreateResponse response = projectService.createProject(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Hidden
    @PatchMapping("/{projectId}")
    @Operation(summary = "프로젝트 정보 수정", description = "프로젝트의 이름과 설명을 수정합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "프로젝트 수정 성공"),
            @ApiResponse(responseCode = "404", description = "프로젝트를 찾을 수 없음")
    })
    public ResponseEntity<Project> updateProject(
            @PathVariable Long projectId,
            @RequestBody ProjectUpdateRequest request
    ) {
        return ResponseEntity.ok(projectService.updateProject(projectId, request));
    }

    @DeleteMapping("/{projectId}")
    @Operation(summary = "프로젝트 삭제", description = "기획자가 특정 프로젝트를 삭제합니다")
    @ApiResponse(responseCode = "204", description = "프로젝트 삭제 성공")
    public ResponseEntity<Void> deleteProject(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long projectId
    ) {
        projectService.deleteProject(userId, projectId);
        return ResponseEntity.noContent().build();
    }

    @Hidden
    @GetMapping("/{projectId}/members")
    @Operation(summary = "프로젝트 멤버 목록 조회", description = "특정 프로젝트의 모든 멤버를 조회합니다")
    @ApiResponse(responseCode = "200", description = "멤버 목록 조회 성공")
    public ResponseEntity<List<ProjectMember>> getProjectMembers(@PathVariable Long projectId) {
        return ResponseEntity.ok(projectMemberRepository.findByProjectId(projectId));
    }

    @Hidden
    @PostMapping("/{projectId}/members")
    @Operation(summary = "프로젝트에 멤버 추가", description = "프로젝트에 새로운 멤버를 추가합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "멤버 추가 성공",
                    content = @Content(schema = @Schema(implementation = ProjectMember.class))),
            @ApiResponse(responseCode = "400", description = "이미 존재하는 멤버"),
            @ApiResponse(responseCode = "403", description = "프로젝트 멤버가 아님"),
            @ApiResponse(responseCode = "404", description = "프로젝트 또는 사용자를 찾을 수 없음")
    })
    public ResponseEntity<ProjectMember> addMember(
            @AuthenticationPrincipal Long currentUserId,
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectMemberAddRequest request
    ) {
        ProjectMember member = projectService.addMember(currentUserId, projectId, request.userId(), request.role());
        return ResponseEntity.status(HttpStatus.CREATED).body(member);
    }

    @PostMapping("/{projectId}/invite")
    @Operation(summary = "프로젝트 멤버 이메일 초대", description = "기획자가 이메일로 프로젝트에 멤버를 초대합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "초대 성공",
                    content = @Content(schema = @Schema(implementation = ProjectInviteResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 또는 이미 존재하는 멤버"),
            @ApiResponse(responseCode = "403", description = "초대 권한 없음 (기획자 아님)"),
            @ApiResponse(responseCode = "404", description = "프로젝트 또는 사용자를 찾을 수 없음")
    })
    public ResponseEntity<ProjectInviteResponse> inviteUser(
            @AuthenticationPrincipal Long inviterId,
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectInviteRequest request
    ) {
        projectService.inviteUserToProject(inviterId, projectId, request.nickname());
        Project project = projectService.getProject(projectId);
        return ResponseEntity.ok(new ProjectInviteResponse(project.getId(), project.getName()));
    }

    @Hidden
    @PatchMapping("/{projectId}/members/{memberId}/role")
    @Operation(summary = "멤버 역할 수정", description = "프로젝트 멤버의 역할을 수정합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "역할 수정 성공"),
            @ApiResponse(responseCode = "404", description = "멤버를 찾을 수 없음")
    })
    public ResponseEntity<ProjectMember> updateMemberRole(
            @PathVariable Long projectId,
            @PathVariable Long memberId,
            @Valid @RequestBody ProjectMemberRoleUpdateRequest request
    ) {
        ProjectMember member = projectMemberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found: " + memberId));

        if (!member.getProjectId().equals(projectId)) {
            throw new IllegalArgumentException("Member does not belong to this project");
        }

        member.updateRole(request.role());
        return ResponseEntity.ok(projectMemberRepository.save(member));
    }

    @Hidden
    @DeleteMapping("/{projectId}/members/{memberId}")
    @Operation(summary = "멤버 삭제", description = "프로젝트에서 멤버를 제거합니다")
    @ApiResponse(responseCode = "204", description = "멤버 삭제 성공")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long projectId,
            @PathVariable Long memberId
    ) {
        ProjectMember member = projectMemberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found: " + memberId));

        if (!member.getProjectId().equals(projectId)) {
            throw new IllegalArgumentException("Member does not belong to this project");
        }

        projectMemberRepository.deleteById(memberId);
        return ResponseEntity.noContent().build();
    }

    @Hidden
    @GetMapping("/{projectId}/members/{userId}")
    @Operation(summary = "특정 사용자의 프로젝트 멤버 조회")
    @ApiResponse(responseCode = "200", description = "멤버 조회 성공")
    public ResponseEntity<ProjectMember> getMember(
            @PathVariable Long projectId,
            @PathVariable Long userId
    ) {
        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found for user: " + userId));
        return ResponseEntity.ok(member);
    }

    @GetMapping("/{projectId}/pipelines")
    @Operation(summary = "프로젝트의 파이프라인 목록 조회", description = "특정 프로젝트에 연결된 파이프라인 목록을 조회합니다. 프로젝트 멤버만 조회 가능합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "파이프라인 조회 성공"),
            @ApiResponse(responseCode = "403", description = "해당 프로젝트의 멤버가 아님"),
            @ApiResponse(responseCode = "404", description = "프로젝트를 찾을 수 없음")
    })
    public ResponseEntity<markoala.fithub.demo.domain.pipeline.dto.response.PipelineListResponse> getProjectPipelines(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long projectId
    ) {
        // 1. 존재하는 프로젝트인지 확인 (존재하지 않으면 404)
        projectService.getProject(projectId);

        // 2. 해당 프로젝트의 멤버인지 확인 (멤버가 아니면 403)
        projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.FORBIDDEN, "해당 프로젝트의 파이프라인을 조회할 권한이 없습니다."
                ));

        // 3. 파이프라인 조회 (AI 서버 호출)
        markoala.fithub.demo.domain.pipeline.dto.response.PipelineListResponse response = pipelineV3Service.getPipelinesByProject(projectId);
        return ResponseEntity.ok(response);
    }
}
