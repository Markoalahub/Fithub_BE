package markoala.fithub.demo.project;

import markoala.fithub.demo.project.dto.ProjectCreateRequest;
import markoala.fithub.demo.project.dto.ProjectUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    public ProjectService(ProjectRepository projectRepository, ProjectMemberRepository projectMemberRepository) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
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
    public Long createProject(ProjectCreateRequest request) {
        Project project = Project.createProject(request.name(), request.description());
        Project savedProject = projectRepository.save(project);
        return savedProject.getId();
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
}
