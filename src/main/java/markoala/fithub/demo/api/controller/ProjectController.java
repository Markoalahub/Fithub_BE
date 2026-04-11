package markoala.fithub.demo.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import markoala.fithub.demo.application.dto.request.ProjectCreateRequest;
import markoala.fithub.demo.application.dto.request.ProjectMemberAddRequest;
import markoala.fithub.demo.application.dto.request.ProjectMemberRoleUpdateRequest;
import markoala.fithub.demo.application.dto.request.ProjectUpdateRequest;
import markoala.fithub.demo.domain.project.entity.Project;
import markoala.fithub.demo.domain.project.entity.ProjectMember;
import markoala.fithub.demo.domain.project.repository.ProjectMemberRepository;
import markoala.fithub.demo.domain.project.repository.ProjectRepository;
import markoala.fithub.demo.domain.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
    public ResponseEntity<Project> getProject(
            @Parameter(description = "프로젝트 ID", required = true)
            @PathVariable Long projectId
    ) {
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
        Project saved = projectRepository.save(project);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PatchMapping("/{projectId}")
    @Operation(summary = "프로젝트 정보 수정", description = "프로젝트의 이름과 설명을 수정합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "프로젝트 수정 성공"),
            @ApiResponse(responseCode = "404", description = "프로젝트를 찾을 수 없음")
    })
    public ResponseEntity<Project> updateProject(
            @Parameter(description = "프로젝트 ID", required = true)
            @PathVariable Long projectId,
            @RequestBody ProjectUpdateRequest request
    ) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        if (request.name() != null && !request.name().isEmpty()) {
            project.updateName(request.name());
        }
        if (request.description() != null && !request.description().isEmpty()) {
            project.updateDescription(request.description());
        }

        Project updated = projectRepository.save(project);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{projectId}")
    @Operation(summary = "프로젝트 삭제", description = "특정 프로젝트를 삭제합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "프로젝트 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "프로젝트를 찾을 수 없음")
    })
    public ResponseEntity<Void> deleteProject(
            @Parameter(description = "프로젝트 ID", required = true)
            @PathVariable Long projectId
    ) {
        projectRepository.deleteById(projectId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{projectId}/members")
    @Operation(summary = "프로젝트 멤버 목록 조회", description = "특정 프로젝트의 모든 멤버를 조회합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "멤버 목록 조회 성공"),
            @ApiResponse(responseCode = "404", description = "프로젝트를 찾을 수 없음")
    })
    public ResponseEntity<List<ProjectMember>> getProjectMembers(
            @Parameter(description = "프로젝트 ID", required = true)
            @PathVariable Long projectId
    ) {
        List<ProjectMember> members = projectMemberRepository.findByProjectId(projectId);
        return ResponseEntity.ok(members);
    }

    @PostMapping("/{projectId}/members")
    @Operation(summary = "프로젝트에 멤버 추가", description = "프로젝트에 새로운 멤버를 추가합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "멤버 추가 성공",
                    content = @Content(schema = @Schema(implementation = ProjectMember.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "404", description = "프로젝트 또는 사용자를 찾을 수 없음")
    })
    public ResponseEntity<ProjectMember> addMember(
            @Parameter(description = "프로젝트 ID", required = true)
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectMemberAddRequest request
    ) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        userRepository.findById(request.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + request.userId()));

        boolean alreadyMember = projectMemberRepository.findByProjectIdAndUserId(projectId, request.userId()).isPresent();
        if (alreadyMember) {
            throw new IllegalArgumentException("User is already a member of this project");
        }

        ProjectMember member = ProjectMember.createMember(projectId, request.userId(), request.role());
        ProjectMember saved = projectMemberRepository.save(member);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PatchMapping("/{projectId}/members/{memberId}/role")
    @Operation(summary = "멤버 역할 수정", description = "프로젝트 멤버의 역할을 수정합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "역할 수정 성공"),
            @ApiResponse(responseCode = "404", description = "멤버를 찾을 수 없음")
    })
    public ResponseEntity<ProjectMember> updateMemberRole(
            @Parameter(description = "프로젝트 ID", required = true)
            @PathVariable Long projectId,
            @Parameter(description = "멤버 ID", required = true)
            @PathVariable Long memberId,
            @Valid @RequestBody ProjectMemberRoleUpdateRequest request
    ) {
        ProjectMember member = projectMemberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found: " + memberId));

        if (!member.getProjectId().equals(projectId)) {
            throw new IllegalArgumentException("Member does not belong to this project");
        }

        member.updateRole(request.role());
        ProjectMember updated = projectMemberRepository.save(member);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{projectId}/members/{memberId}")
    @Operation(summary = "멤버 삭제", description = "프로젝트에서 멤버를 제거합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "멤버 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "멤버를 찾을 수 없음")
    })
    public ResponseEntity<Void> removeMember(
            @Parameter(description = "프로젝트 ID", required = true)
            @PathVariable Long projectId,
            @Parameter(description = "멤버 ID", required = true)
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
    @Operation(summary = "특정 사용자의 프로젝트 멤버 조회", description = "프로젝트 내 특정 사용자의 멤버 정보를 조회합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "멤버 조회 성공",
                    content = @Content(schema = @Schema(implementation = ProjectMember.class))),
            @ApiResponse(responseCode = "404", description = "멤버를 찾을 수 없음")
    })
    public ResponseEntity<ProjectMember> getMember(
            @Parameter(description = "프로젝트 ID", required = true)
            @PathVariable Long projectId,
            @Parameter(description = "사용자 ID", required = true)
            @PathVariable Long userId
    ) {
        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found for user: " + userId));
        return ResponseEntity.ok(member);
    }
}
