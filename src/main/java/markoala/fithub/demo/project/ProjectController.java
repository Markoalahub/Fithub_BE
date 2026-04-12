package markoala.fithub.demo.project;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import markoala.fithub.demo.project.dto.ProjectCreateRequest;
import markoala.fithub.demo.project.dto.ProjectMemberAddRequest;
import markoala.fithub.demo.project.dto.ProjectMemberRoleUpdateRequest;
import markoala.fithub.demo.project.dto.ProjectUpdateRequest;
import markoala.fithub.demo.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
@Tag(name = "Projects", description = "프로젝트 관리 및 멤버 관리 API")
public class ProjectController {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;

    public ProjectController(
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            UserRepository userRepository
    ) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/{projectId}")
    @Operation(summary = "프로젝트 조회", description = "특정 프로젝트의 상세 정보를 조회합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "프로젝트 조회 성공",
                    content = @Content(schema = @Schema(implementation = Project.class))),
            @ApiResponse(responseCode = "404", description = "프로젝트를 찾을 수 없음")
    })
    public ResponseEntity<Project> getProject(@PathVariable Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        return ResponseEntity.ok(project);
    }

    @PostMapping
    @Operation(summary = "프로젝트 생성", description = "새로운 프로젝트를 생성합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "프로젝트 생성 성공",
                    content = @Content(schema = @Schema(implementation = Project.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터")
    })
    public ResponseEntity<Project> createProject(
            @Valid @RequestBody ProjectCreateRequest request
    ) {
        Project project = Project.createProject(request.name(), request.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(projectRepository.save(project));
    }

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
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        if (request.name() != null && !request.name().isBlank()) project.updateName(request.name());
        if (request.description() != null && !request.description().isBlank()) project.updateDescription(request.description());

        return ResponseEntity.ok(projectRepository.save(project));
    }

    @DeleteMapping("/{projectId}")
    @Operation(summary = "프로젝트 삭제", description = "특정 프로젝트를 삭제합니다")
    @ApiResponse(responseCode = "204", description = "프로젝트 삭제 성공")
    public ResponseEntity<Void> deleteProject(@PathVariable Long projectId) {
        projectRepository.deleteById(projectId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{projectId}/members")
    @Operation(summary = "프로젝트 멤버 목록 조회", description = "특정 프로젝트의 모든 멤버를 조회합니다")
    @ApiResponse(responseCode = "200", description = "멤버 목록 조회 성공")
    public ResponseEntity<List<ProjectMember>> getProjectMembers(@PathVariable Long projectId) {
        return ResponseEntity.ok(projectMemberRepository.findByProjectId(projectId));
    }

    @PostMapping("/{projectId}/members")
    @Operation(summary = "프로젝트에 멤버 추가", description = "프로젝트에 새로운 멤버를 추가합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "멤버 추가 성공",
                    content = @Content(schema = @Schema(implementation = ProjectMember.class))),
            @ApiResponse(responseCode = "400", description = "이미 존재하는 멤버"),
            @ApiResponse(responseCode = "404", description = "프로젝트 또는 사용자를 찾을 수 없음")
    })
    public ResponseEntity<ProjectMember> addMember(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectMemberAddRequest request
    ) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        userRepository.findById(request.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + request.userId()));

        if (projectMemberRepository.findByProjectIdAndUserId(projectId, request.userId()).isPresent()) {
            throw new IllegalArgumentException("User is already a member of this project");
        }

        ProjectMember member = ProjectMember.createMember(projectId, request.userId(), request.role());
        return ResponseEntity.status(HttpStatus.CREATED).body(projectMemberRepository.save(member));
    }

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
}
