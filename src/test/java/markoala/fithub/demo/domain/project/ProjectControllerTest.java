package markoala.fithub.demo.domain.project;

import com.fasterxml.jackson.databind.ObjectMapper;
import markoala.fithub.demo.domain.pipeline.service.PipelineV3Service;
import markoala.fithub.demo.global.config.SecurityConfig;
import markoala.fithub.demo.global.security.jwt.JwtProvider;
import markoala.fithub.demo.domain.project.dto.ProjectCreateRequest;
import markoala.fithub.demo.domain.project.dto.ProjectCreateResponse;
import markoala.fithub.demo.domain.project.dto.ProjectInviteRequest;
import markoala.fithub.demo.domain.project.exception.DuplicateProjectException;
import markoala.fithub.demo.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProjectController.class)
@Import(SecurityConfig.class)
public class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProjectService projectService;

    @MockBean
    private ProjectMemberRepository projectMemberRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private PipelineV3Service pipelineV3Service;

    @MockBean
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        when(jwtProvider.validateToken(anyString())).thenReturn(true);
        when(jwtProvider.getUserIdFromToken(anyString())).thenReturn(1L);
    }

    @Test
    @DisplayName("프로젝트 생성 성공")
    void createProject_Success() throws Exception {
        ProjectCreateRequest request = new ProjectCreateRequest("Fithub", "Project description");
        ProjectCreateResponse response = new ProjectCreateResponse(1L, "Fithub");

        when(projectService.createProject(eq(1L), any(ProjectCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/projects")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.project_id").value(1L))
                .andExpect(jsonPath("$.project_name").value("Fithub"));
    }

    @Test
    @DisplayName("프로젝트 생성 실패 - 필수 값 누락 (400)")
    void createProject_ValidationFail() throws Exception {
        ProjectCreateRequest request = new ProjectCreateRequest("", "desc");

        mockMvc.perform(post("/projects")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("프로젝트 생성 실패 - 중복 이름 (409)")
    void createProject_DuplicateName() throws Exception {
        ProjectCreateRequest request = new ProjectCreateRequest("Fithub", "Project description");

        when(projectService.createProject(eq(1L), any(ProjectCreateRequest.class)))
                .thenThrow(new DuplicateProjectException("이미 동일한 이름의 프로젝트에 참여하고 있습니다."));

        mockMvc.perform(post("/projects")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("이미 동일한 이름의 프로젝트에 참여하고 있습니다."));
    }

    @Test
    @DisplayName("내 프로젝트 조회 성공")
    void getMyProjects_Success() throws Exception {
        when(projectService.getUserProjects(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/projects/me")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("프로젝트 초대 성공")
    void inviteUserToProject_Success() throws Exception {
        ProjectInviteRequest request = new ProjectInviteRequest("newMember");
        Project project = Project.createProject("Fithub", "desc", 1L);
        
        doNothing().when(projectService).inviteUserToProject(1L, 1L, "newMember");
        when(projectService.getProject(1L)).thenReturn(project);

        mockMvc.perform(post("/projects/1/invite")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("프로젝트 초대 실패 - 권한 없음 (409 Conflict mapped from IllegalState)")
    void inviteUserToProject_Forbidden() throws Exception {
        ProjectInviteRequest request = new ProjectInviteRequest("newMember");

        doThrow(new IllegalStateException("프로젝트 초대 권한이 없습니다."))
                .when(projectService).inviteUserToProject(1L, 1L, "newMember");

        mockMvc.perform(post("/projects/1/invite")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict()); 
    }

    @Test
    @DisplayName("프로젝트 삭제 성공")
    void deleteProject_Success() throws Exception {
        doNothing().when(projectService).deleteProject(1L, 1L);

        mockMvc.perform(delete("/projects/1")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("프로젝트 삭제 실패 - 기획자가 아님 (403)")
    void deleteProject_Forbidden() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "기획자만 프로젝트를 삭제할 수 있습니다."))
                .when(projectService).deleteProject(1L, 1L);

        mockMvc.perform(delete("/projects/1")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("내 프로젝트 파이프라인 조회 실패 - 멤버 아님 (403)")
    void getProjectPipelines_Forbidden() throws Exception {
        when(projectService.getProject(1L)).thenReturn(null);
        when(projectMemberRepository.findByProjectIdAndUserId(1L, 1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/projects/1/pipelines")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isForbidden());
    }
}
