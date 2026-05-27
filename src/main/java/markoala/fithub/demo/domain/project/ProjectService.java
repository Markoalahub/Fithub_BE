package markoala.fithub.demo.domain.project;

import markoala.fithub.demo.domain.project.dto.ProjectCreateRequest;
import markoala.fithub.demo.domain.project.dto.ProjectUpdateRequest;
import markoala.fithub.demo.domain.user.JobRole;
import markoala.fithub.demo.domain.user.User;
import markoala.fithub.demo.domain.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;

    public Project getProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "존재하지 않는 프로젝트 ID 입니다."));
    }

    public List<Project> getUserProjects(Long userId) {
        List<Long> projectIds = projectMemberRepository.findByUserId(userId)
                .stream()
                .map(ProjectMember::getProjectId)
                .toList();
        return projectRepository.findAllById(projectIds);
    }

    @Transactional
    public markoala.fithub.demo.domain.project.dto.ProjectCreateResponse createProject(Long userId, ProjectCreateRequest request) {
        if (userId == null) {
            throw new IllegalStateException("인증된 사용자만 프로젝트를 생성할 수 있습니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (user.getJobRole() != JobRole.PLANNER) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "기획자만 프로젝트를 생성할 수 있습니다."
            );
        }

        boolean isDuplicate = getUserProjects(userId).stream()
                .anyMatch(p -> p.getName().equals(request.name()));
        
        if (isDuplicate) {
            throw new markoala.fithub.demo.domain.project.exception.DuplicateProjectException("이미 동일한 이름의 프로젝트에 참여하고 있습니다.");
        }

        Project project = Project.createProject(request.name(), request.description(), userId);
        Project savedProject = projectRepository.save(project);

        ProjectMember creator = ProjectMember.createMember(savedProject.getId(), userId, user.getJobRole().name());
        projectMemberRepository.save(creator);

        return new markoala.fithub.demo.domain.project.dto.ProjectCreateResponse(savedProject.getId(), savedProject.getName());
    }

    @Transactional
    public Project updateProject(Long projectId, ProjectUpdateRequest request) {
        Project project = getProject(projectId);

        if (request.name() != null && !request.name().isBlank()) {
            project.updateName(request.name());
        }
        if (request.description() != null && !request.description().isBlank()) {
            project.updateDescription(request.description());
        }

        return projectRepository.save(project);
    }

    @Transactional
    public void deleteProject(Long userId, Long projectId) {
        // 1. 프로젝트 존재 여부를 먼저 확인하여 존재하지 않으면 404 반환
        Project project = getProject(projectId);

        // 2. 프로젝트의 주인이 삭제를 요청하는지 확인 (주인이 아니면 403 반환)
        if (!project.getCreatorId().equals(userId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "프로젝트를 생성한 사람만 삭제할 수 있습니다."
            );
        }

        List<ProjectMember> members = projectMemberRepository.findByProjectId(projectId);
        projectMemberRepository.deleteAll(members);

        projectRepository.delete(project);
    }

    @Transactional
    public void inviteUserToProject(Long inviterId, Long projectId, String nickname) {
        User inviter = userRepository.findById(inviterId)
                .orElseThrow(() -> new IllegalArgumentException("Inviter not found: " + inviterId));

        boolean isPlanner = inviter.getJobRole() == JobRole.PLANNER;
        boolean isProjectMember = projectMemberRepository.findByProjectIdAndUserId(projectId, inviterId).isPresent();

        if (!isPlanner && !isProjectMember) {
            throw new IllegalStateException("프로젝트 초대 권한이 없습니다.");
        }

        Project project = getProject(projectId);

        User user = userRepository.findByNickname(nickname)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."));

        projectMemberRepository.findByProjectIdAndUserId(projectId, user.getId())
                .ifPresent(m -> {
                    throw new IllegalStateException("User is already a member of this project.");
                });

        String userRole = user.getJobRole() != null ? user.getJobRole().name() : "MEMBER";
        ProjectMember projectMember = ProjectMember.createMember(project.getId(), user.getId(), userRole);
        projectMemberRepository.save(projectMember);
    }

    @Transactional
    public ProjectMember addMember(Long currentUserId, Long projectId, Long userId, String role) {
        projectMemberRepository.findByProjectIdAndUserId(projectId, currentUserId)
                .orElseThrow(() -> new IllegalStateException("Only existing members can add new members to this project."));

        Project project = getProject(projectId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .ifPresent(m -> {
                    throw new IllegalArgumentException("User is already a member of this project");
                });

        ProjectMember member = ProjectMember.createMember(project.getId(), user.getId(), role);
        return projectMemberRepository.save(member);
    }
}
