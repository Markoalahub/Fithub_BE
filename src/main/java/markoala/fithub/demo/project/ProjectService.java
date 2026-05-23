package markoala.fithub.demo.project;

import markoala.fithub.demo.project.dto.ProjectCreateRequest;
import markoala.fithub.demo.project.dto.ProjectUpdateRequest;
import markoala.fithub.demo.user.JobRole;
import markoala.fithub.demo.user.User;
import markoala.fithub.demo.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;

    public ProjectService(ProjectRepository projectRepository, ProjectMemberRepository projectMemberRepository, UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.userRepository = userRepository;
    }

    public Project getProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
    }

    public List<Project> getUserProjects(Long userId) {
        List<Long> projectIds = projectMemberRepository.findByUserId(userId)
                .stream()
                .map(ProjectMember::getProjectId)
                .toList();
        return projectRepository.findAllById(projectIds);
    }

    @Transactional
    public markoala.fithub.demo.project.dto.ProjectCreateResponse createProject(Long userId, ProjectCreateRequest request) {
        if (userId == null) {
            throw new IllegalStateException("인증된 사용자만 프로젝트를 생성할 수 있습니다.");
        }

        boolean isDuplicate = getUserProjects(userId).stream()
                .anyMatch(p -> p.getName().equals(request.name()));
        
        if (isDuplicate) {
            throw new markoala.fithub.demo.project.exception.DuplicateProjectException("이미 동일한 이름의 프로젝트에 참여하고 있습니다.");
        }

        Project project = Project.createProject(request.name(), request.description());
        Project savedProject = projectRepository.save(project);

        ProjectMember creator = ProjectMember.createMember(savedProject.getId(), userId, "PLANNER");
        projectMemberRepository.save(creator);

        return new markoala.fithub.demo.project.dto.ProjectCreateResponse(savedProject.getId(), savedProject.getName());
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
    public void deleteProject(Long projectId) {
        projectRepository.deleteById(projectId);
    }

    @Transactional
    public void inviteUserToProject(Long inviterId, Long projectId, String email, String role) {
        User inviter = userRepository.findById(inviterId)
                .orElseThrow(() -> new IllegalArgumentException("Inviter not found: " + inviterId));

        if (inviter.getJobRole() != JobRole.PLANNER) {
            throw new IllegalStateException("Only a PLANNER can invite users to a project.");
        }

        Project project = getProject(projectId);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));

        projectMemberRepository.findByProjectIdAndUserId(projectId, user.getId())
                .ifPresent(m -> {
                    throw new IllegalStateException("User is already a member of this project.");
                });

        ProjectMember projectMember = ProjectMember.createMember(project.getId(), user.getId(), role);
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
