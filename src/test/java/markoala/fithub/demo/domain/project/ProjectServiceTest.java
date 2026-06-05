package markoala.fithub.demo.domain.project;

import markoala.fithub.demo.domain.outbox.OutboxEventRepository;
import markoala.fithub.demo.domain.outbox.OutboxEventType;
import markoala.fithub.demo.domain.project.dto.ProjectCreateRequest;
import markoala.fithub.demo.domain.project.dto.ProjectDetailResponse;
import markoala.fithub.demo.domain.project.dto.ProjectUpdateRequest;
import markoala.fithub.demo.domain.project.dto.ProjectCreateResponse;
import markoala.fithub.demo.domain.project.exception.DuplicateProjectException;
import markoala.fithub.demo.domain.user.JobRole;
import markoala.fithub.demo.domain.user.User;
import markoala.fithub.demo.domain.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @InjectMocks
    private ProjectService projectService;

    @Test
    @DisplayName("getProject - 성공")
    void getProject_Success() {
        Project project = new Project(1L, "Test Project", "Desc", 1L, null, null);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        Project result = projectService.getProject(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Test Project");
    }

    @Test
    @DisplayName("getProject - 실패 (프로젝트 없음)")
    void getProject_NotFound() {
        when(projectRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getProject(1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("존재하지 않는 프로젝트 ID 입니다.");
    }

    @Test
    @DisplayName("getUserProjects - 성공")
    void getUserProjects_Success() {
        ProjectMember member = new ProjectMember(1L, 100L, 1L, "PLANNER", null, null);
        Project project = new Project(100L, "Test Project", "Desc", 1L, null, null);

        when(projectMemberRepository.findByUserId(1L)).thenReturn(List.of(member));
        when(projectRepository.findAllById(List.of(100L))).thenReturn(List.of(project));

        List<Project> results = projectService.getUserProjects(1L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("getProjectDetail - 성공")
    void getProjectDetail_Success() {
        Project project = new Project(100L, "Fithub", "Project description", 1L, null, null);
        ProjectMember plannerMember = new ProjectMember(10L, 100L, 1L, "PLANNER", null, null);
        ProjectMember backendMember = new ProjectMember(11L, 100L, 2L, "BACKEND", null, null);
        User planner = new User(1L, "plannerUser", "planner", "planner@test.com", "social1", true, null, null, null, null);
        User backend = new User(2L, "backendUser", "backend", "backend@test.com", "social2", true, null, null, null, null);

        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findByProjectId(100L)).thenReturn(List.of(plannerMember, backendMember));
        when(userRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(planner, backend));

        ProjectDetailResponse result = projectService.getProjectDetail(100L);

        assertThat(result.projectId()).isEqualTo(100L);
        assertThat(result.projectName()).isEqualTo("Fithub");
        assertThat(result.projectDescription()).isEqualTo("Project description");
        assertThat(result.memberCount()).isEqualTo(2);
        assertThat(result.members())
                .extracting("userId", "nickname")
                .containsExactly(
                        tuple(1L, "planner"),
                        tuple(2L, "backend")
                );

        verify(projectRepository).findById(100L);
        verify(projectMemberRepository).findByProjectId(100L);
        verify(userRepository).findAllById(List.of(1L, 2L));
    }

    @Test
    @DisplayName("getProjectDetail - 멤버 사용자 정보가 없으면 닉네임 null로 반환")
    void getProjectDetail_MissingUserNickname() {
        Project project = new Project(100L, "Fithub", "Project description", 1L, null, null);
        ProjectMember plannerMember = new ProjectMember(10L, 100L, 1L, "PLANNER", null, null);
        ProjectMember backendMember = new ProjectMember(11L, 100L, 2L, "BACKEND", null, null);
        User planner = new User(1L, "plannerUser", "planner", "planner@test.com", "social1", true, null, null, null, null);

        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findByProjectId(100L)).thenReturn(List.of(plannerMember, backendMember));
        when(userRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(planner));

        ProjectDetailResponse result = projectService.getProjectDetail(100L);

        assertThat(result.memberCount()).isEqualTo(2);
        assertThat(result.members())
                .extracting("userId", "nickname")
                .containsExactly(
                        tuple(1L, "planner"),
                        tuple(2L, null)
                );

        verify(projectRepository).findById(100L);
        verify(projectMemberRepository).findByProjectId(100L);
        verify(userRepository).findAllById(List.of(1L, 2L));
    }

    @Test
    @DisplayName("getProjectDetail - 실패 (프로젝트 없음)")
    void getProjectDetail_NotFound() {
        when(projectRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getProjectDetail(100L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("존재하지 않는 프로젝트 ID 입니다.");

        verify(projectRepository).findById(100L);
        verify(projectMemberRepository, never()).findByProjectId(anyLong());
        verify(userRepository, never()).findAllById(any());
    }

    @Test
    @DisplayName("createProject - 성공")
    void createProject_Success() {
        ProjectCreateRequest request = new ProjectCreateRequest("New Project", "Desc");
        User user = new User(1L, "user1", "nick1", "email@test.com", "social1", true, null, null, null, null);
        user.updateJobRole(JobRole.PLANNER);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(projectMemberRepository.findByUserId(1L)).thenReturn(Collections.emptyList());
        when(projectRepository.findAllById(Collections.emptyList())).thenReturn(Collections.emptyList());
        
        Project savedProject = new Project(10L, "New Project", "Desc", 1L, "nick1", null, null);
        when(projectRepository.save(any(Project.class))).thenReturn(savedProject);

        ProjectCreateResponse response = projectService.createProject(1L, request);

        assertThat(response.projectId()).isEqualTo(10L);
        assertThat(response.projectName()).isEqualTo("New Project");
        assertThat(response.creatorId()).isEqualTo(1L);
        assertThat(response.creatorNickname()).isEqualTo("nick1");
        verify(projectMemberRepository).save(any(ProjectMember.class));
    }

    @Test
    @DisplayName("createProject - 실패 (userId null)")
    void createProject_NullUserId() {
        ProjectCreateRequest request = new ProjectCreateRequest("New Project", "Desc");

        assertThatThrownBy(() -> projectService.createProject(null, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("인증된 사용자만 프로젝트를 생성할 수 있습니다.");
    }

    @Test
    @DisplayName("createProject - 실패 (사용자 없음)")
    void createProject_UserNotFound() {
        ProjectCreateRequest request = new ProjectCreateRequest("New Project", "Desc");
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.createProject(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("createProject - 실패 (기획자가 아님)")
    void createProject_NotPlanner() {
        ProjectCreateRequest request = new ProjectCreateRequest("New Project", "Desc");
        User user = new User(1L, "user1", "nick1", "email@test.com", "social1", true, null, null, null, null);
        user.updateJobRole(JobRole.BACKEND);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> projectService.createProject(1L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("기획자만 프로젝트를 생성할 수 있습니다");
    }

    @Test
    @DisplayName("createProject - 실패 (중복 프로젝트)")
    void createProject_Duplicate() {
        ProjectCreateRequest request = new ProjectCreateRequest("Existing Project", "Desc");
        User user = new User(1L, "user1", "nick1", "email@test.com", "social1", true, null, null, null, null);
        user.updateJobRole(JobRole.PLANNER);

        ProjectMember member = new ProjectMember(1L, 100L, 1L, "PLANNER", null, null);
        Project project = new Project(100L, "Existing Project", "Desc", 1L, null, null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(projectMemberRepository.findByUserId(1L)).thenReturn(List.of(member));
        when(projectRepository.findAllById(List.of(100L))).thenReturn(List.of(project));

        assertThatThrownBy(() -> projectService.createProject(1L, request))
                .isInstanceOf(DuplicateProjectException.class)
                .hasMessageContaining("이미 동일한 이름의 프로젝트에 참여하고 있습니다");
    }

    @Test
    @DisplayName("updateProject - 성공 (전부 업데이트)")
    void updateProject_Success() {
        ProjectUpdateRequest request = new ProjectUpdateRequest("Updated Name", "Updated Desc");
        Project project = new Project(1L, "Old Name", "Old Desc", 1L, null, null);

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectRepository.save(project)).thenReturn(project);

        Project result = projectService.updateProject(1L, request);

        assertThat(result.getName()).isEqualTo("Updated Name");
        assertThat(result.getDescription()).isEqualTo("Updated Desc");
    }

    @Test
    @DisplayName("updateProject - 성공 (null 처리)")
    void updateProject_NullHandling() {
        ProjectUpdateRequest request = new ProjectUpdateRequest(null, "");
        Project project = new Project(1L, "Old Name", "Old Desc", 1L, null, null);

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectRepository.save(project)).thenReturn(project);

        Project result = projectService.updateProject(1L, request);

        assertThat(result.getName()).isEqualTo("Old Name");
        assertThat(result.getDescription()).isEqualTo("Old Desc");
    }

    @Test
    @DisplayName("deleteProject - 성공")
    void deleteProject_Success() {
        Project project = new Project(1L, "Old Name", "Old Desc", 1L, null, null);
        ProjectMember member = new ProjectMember(1L, 1L, 1L, "PLANNER", null, null);

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectMemberRepository.findByProjectId(1L)).thenReturn(List.of(member));

        projectService.deleteProject(1L, 1L);

        verify(projectMemberRepository).deleteAll(List.of(member));
        verify(projectRepository).delete(project);
        verify(outboxEventRepository).save(argThat(event ->
                event.getEventType() == OutboxEventType.PROJECT_DELETED
                        && event.getAggregateId().equals(1L)
        ));
    }

    @Test
    @DisplayName("deleteProject - 실패 (작성자가 아님)")
    void deleteProject_NotCreator() {
        Project project = new Project(1L, "Old Name", "Old Desc", 2L, null, null);

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectService.deleteProject(1L, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("프로젝트를 생성한 사람만 삭제할 수 있습니다");
    }

    @Test
    @DisplayName("inviteUserToProject - 성공")
    void inviteUserToProject_Success() {
        Project project = new Project(100L, "Proj", "Desc", 1L, null, null);
        User invitee = new User(2L, "user2", "nick2", "email2", "s2", true, null, null, null, null);
        invitee.updateJobRole(JobRole.BACKEND);

        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
        when(userRepository.findByNickname("nick2")).thenReturn(Optional.of(invitee));
        lenient().when(projectMemberRepository.findByProjectIdAndUserId(100L, 2L)).thenReturn(Optional.empty());

        projectService.inviteUserToProject(1L, 100L, "nick2");

        verify(projectMemberRepository).save(any(ProjectMember.class));
    }

    @Test
    @DisplayName("inviteUserToProject - 실패 (생성자가 아닌 멤버가 초대)")
    void inviteUserToProject_MemberCannotInvite() {
        Project project = new Project(100L, "Proj", "Desc", 2L, null, null);

        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectService.inviteUserToProject(1L, 100L, "nick2"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("프로젝트를 생성한 기획자만 멤버를 초대할 수 있습니다");

        verify(projectMemberRepository, never()).save(any(ProjectMember.class));
    }

    @Test
    @DisplayName("inviteUserToProject - 실패 (권한 없음)")
    void inviteUserToProject_NoPermission() {
        Project project = new Project(100L, "Proj", "Desc", 2L, null, null);

        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectService.inviteUserToProject(1L, 100L, "nick2"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("프로젝트를 생성한 기획자만 멤버를 초대할 수 있습니다");
    }

    @Test
    @DisplayName("inviteUserToProject - 실패 (이미 멤버)")
    void inviteUserToProject_AlreadyMember() {
        Project project = new Project(100L, "Proj", "Desc", 1L, null, null);
        User invitee = new User(2L, "user2", "nick2", "email2", "s2", true, null, null, null, null);

        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
        when(userRepository.findByNickname("nick2")).thenReturn(Optional.of(invitee));
        lenient().when(projectMemberRepository.findByProjectIdAndUserId(100L, 2L)).thenReturn(Optional.of(ProjectMember.createMember(100L, 1L, "MEMBER")));

        assertThatThrownBy(() -> projectService.inviteUserToProject(1L, 100L, "nick2"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 이 프로젝트의 멤버입니다.");
    }

    @Test
    @DisplayName("addMember - 성공")
    void addMember_Success() {
        Project project = new Project(100L, "Proj", "Desc", 1L, null, null);
        User user = new User(2L, "user2", "nick2", "email2", "s2", true, null, null, null, null);

        lenient().when(projectMemberRepository.findByProjectIdAndUserId(100L, 1L)).thenReturn(Optional.of(ProjectMember.createMember(100L, 1L, "MEMBER")));
        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        lenient().when(projectMemberRepository.findByProjectIdAndUserId(100L, 2L)).thenReturn(Optional.empty());
        
        ProjectMember member = new ProjectMember(10L, 100L, 2L, "BACKEND", null, null);
        when(projectMemberRepository.save(any())).thenReturn(member);

        ProjectMember result = projectService.addMember(1L, 100L, 2L, "BACKEND");

        assertThat(result.getId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("addMember - 실패 (초대자 권한 없음)")
    void addMember_NoPermission() {
        lenient().when(projectMemberRepository.findByProjectIdAndUserId(100L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.addMember(1L, 100L, 2L, "BACKEND"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("기존 멤버만 이 프로젝트에 새 멤버를 초대할 수 있습니다.");
    }
    
    @Test
    @DisplayName("addMember - 실패 (이미 멤버)")
    void addMember_AlreadyMember() {
        Project project = new Project(100L, "Proj", "Desc", 1L, null, null);
        User user = new User(2L, "user2", "nick2", "email2", "s2", true, null, null, null, null);

        lenient().when(projectMemberRepository.findByProjectIdAndUserId(100L, 1L)).thenReturn(Optional.of(ProjectMember.createMember(100L, 1L, "MEMBER")));
        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        lenient().when(projectMemberRepository.findByProjectIdAndUserId(100L, 2L)).thenReturn(Optional.of(ProjectMember.createMember(100L, 1L, "MEMBER")));

        assertThatThrownBy(() -> projectService.addMember(1L, 100L, 2L, "BACKEND"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 이 프로젝트의 멤버입니다.");
    }
}
