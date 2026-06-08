package markoala.fithub.demo.domain.project;

import com.fasterxml.jackson.databind.ObjectMapper;
import markoala.fithub.demo.domain.pipeline.dto.response.FeatResponse;
import markoala.fithub.demo.domain.pipeline.dto.response.PipelineSummaryResponse;
import markoala.fithub.demo.domain.pipeline.dto.response.PipelineV3Response;
import markoala.fithub.demo.domain.pipeline.dto.response.ProjectPipelineSummaryListResponse;
import markoala.fithub.demo.domain.pipeline.service.PipelineV3Service;
import markoala.fithub.demo.global.config.SecurityConfig;
import markoala.fithub.demo.global.security.jwt.JwtProvider;
import markoala.fithub.demo.domain.project.dto.ProjectCreateRequest;
import markoala.fithub.demo.domain.project.dto.ProjectCreateResponse;
import markoala.fithub.demo.domain.project.dto.ProjectDetailMemberResponse;
import markoala.fithub.demo.domain.project.dto.ProjectDetailResponse;
import markoala.fithub.demo.domain.project.dto.ProjectInviteRequest;
import markoala.fithub.demo.domain.project.dto.ProjectUpdateRequest;
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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;

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
        when(jwtProvider.validateAccessToken(anyString())).thenReturn(true);
        when(jwtProvider.getUserIdFromToken(anyString())).thenReturn(1L);
    }

    @Test
    @DisplayName("프로젝트 생성 성공")
    void createProject_Success() throws Exception {
        ProjectCreateRequest request = new ProjectCreateRequest("Fithub", "Project description");
        ProjectCreateResponse response = new ProjectCreateResponse(1L, "Fithub", 1L, "planner");

        when(projectService.createProject(eq(1L), any(ProjectCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/projects")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.project_id").value(1L))
                .andExpect(jsonPath("$.project_name").value("Fithub"))
                .andExpect(jsonPath("$.creator_id").value(1L))
                .andExpect(jsonPath("$.creator_nickname").value("planner"));
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
    @DisplayName("프로젝트 상세 조회 성공")
    void getProjectDetail_Success() throws Exception {
        ProjectDetailResponse response = new ProjectDetailResponse(
                1L,
                "Fithub",
                "Project description",
                List.of(
                        new ProjectDetailMemberResponse(1L, "planner"),
                        new ProjectDetailMemberResponse(2L, "backend")
                ),
                2
        );

        when(projectService.getProjectDetail(1L)).thenReturn(response);

        mockMvc.perform(get("/projects/1")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.project_id").value(1L))
                .andExpect(jsonPath("$.project_name").value("Fithub"))
                .andExpect(jsonPath("$.project_description").value("Project description"))
                .andExpect(jsonPath("$.members[0].user_id").value(1L))
                .andExpect(jsonPath("$.members[0].nickname").value("planner"))
                .andExpect(jsonPath("$.members[1].user_id").value(2L))
                .andExpect(jsonPath("$.members[1].nickname").value("backend"))
                .andExpect(jsonPath("$.member_count").value(2));

        verify(projectService).getProjectDetail(1L);
    }

    @Test
    @DisplayName("프로젝트 상세 조회 실패 - 프로젝트 없음 (404)")
    void getProjectDetail_NotFound() throws Exception {
        when(projectService.getProjectDetail(1L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 프로젝트 ID 입니다."));

        mockMvc.perform(get("/projects/1")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("존재하지 않는 프로젝트 ID 입니다."));

        verify(projectService).getProjectDetail(1L);
    }

    @Test
    @DisplayName("프로젝트 수정 성공")
    void updateProject_Success() throws Exception {
        ProjectUpdateRequest request = new ProjectUpdateRequest("Fithub 리뉴얼", "AI 협업 기능을 강화한 프로젝트");
        Project updatedProject = new Project(1L, "Fithub 리뉴얼", "AI 협업 기능을 강화한 프로젝트", 1L, null, null);

        when(projectService.updateProject(eq(1L), eq(1L), any(ProjectUpdateRequest.class))).thenReturn(updatedProject);

        mockMvc.perform(patch("/projects/1")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.project_id").value(1L))
                .andExpect(jsonPath("$.project_name").value("Fithub 리뉴얼"))
                .andExpect(jsonPath("$.project_description").value("AI 협업 기능을 강화한 프로젝트"))
                .andExpect(jsonPath("$.creator_id").value(1L));

        verify(projectService).updateProject(eq(1L), eq(1L), argThat(updateRequest ->
                updateRequest != null
                        && "Fithub 리뉴얼".equals(updateRequest.name())
                        && "AI 협업 기능을 강화한 프로젝트".equals(updateRequest.description())
        ));
    }

    @Test
    @DisplayName("프로젝트 수정 실패 - 생성자가 아님 (403)")
    void updateProject_ForbiddenWhenNotCreator() throws Exception {
        ProjectUpdateRequest request = new ProjectUpdateRequest("Fithub 리뉴얼", "AI 협업 기능을 강화한 프로젝트");

        when(projectService.updateProject(eq(1L), eq(1L), any(ProjectUpdateRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "프로젝트를 생성한 사람만 수정할 수 있습니다."));

        mockMvc.perform(patch("/projects/1")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("프로젝트를 생성한 사람만 수정할 수 있습니다."));

        verify(projectService).updateProject(eq(1L), eq(1L), any(ProjectUpdateRequest.class));
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
    @DisplayName("프로젝트 초대 실패 - 권한 없음 (403)")
    void inviteUserToProject_Forbidden() throws Exception {
        ProjectInviteRequest request = new ProjectInviteRequest("newMember");

        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "프로젝트를 생성한 기획자만 멤버를 초대할 수 있습니다."))
                .when(projectService).inviteUserToProject(1L, 1L, "newMember");

        mockMvc.perform(post("/projects/1/invite")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
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
    @DisplayName("프로젝트 파이프라인 조회 - 카테고리 없으면 요약 목록 조회")
    void getProjectPipelines_SummaryWithoutCategory() throws Exception {
        Project project = Project.createProject("Fithub", "desc", 1L);
        ProjectPipelineSummaryListResponse response = new ProjectPipelineSummaryListResponse(
                1L,
                List.of(new PipelineSummaryResponse(33L, "FE 파이프라인 33", "FE")),
                1L
        );

        when(projectService.getProject(1L)).thenReturn(project);
        when(pipelineV3Service.getProjectPipelineSummaries(1L)).thenReturn(response);

        mockMvc.perform(get("/projects/1/pipelines")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.project_id").value(1L))
                .andExpect(jsonPath("$.total").value(1L))
                .andExpect(jsonPath("$.pipelines[0].pipe_id").value(33L))
                .andExpect(jsonPath("$.pipelines[0].pipeline_name").value("FE 파이프라인 33"))
                .andExpect(jsonPath("$.pipelines[0].category").value("FE"));

        verify(projectService).getProject(1L);
        verify(pipelineV3Service).getProjectPipelineSummaries(1L);
        verify(pipelineV3Service, never()).getLatestProjectPipeline(anyLong(), anyString());
        verify(projectMemberRepository, never()).findByProjectIdAndUserId(anyLong(), anyLong());
    }

    @Test
    @DisplayName("프로젝트 파이프라인 조회 - 카테고리가 있으면 최신 상세 조회")
    void getProjectPipelines_LatestByCategory() throws Exception {
        Project project = Project.createProject("Fithub", "desc", 1L);
        PipelineV3Response response = new PipelineV3Response(
                33L,
                1L,
                "FE",
                15,
                "React, expo",
                List.of(new FeatResponse(
                        225L,
                        "[UI 컴포넌트] 사용자 입력 폼 개발",
                        List.of("[UI] 총 예산 상한가 입력 필드 컴포넌트 개발"),
                        1
                ))
        );

        when(projectService.getProject(1L)).thenReturn(project);
        when(pipelineV3Service.getLatestProjectPipeline(1L, "FE")).thenReturn(response);

        mockMvc.perform(get("/projects/1/pipelines")
                        .param("category", "FE")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pipe_id").value(33L))
                .andExpect(jsonPath("$.project_id").value(1L))
                .andExpect(jsonPath("$.category").value("FE"))
                .andExpect(jsonPath("$.version").value(15))
                .andExpect(jsonPath("$.tech_stack").value("React, expo"))
                .andExpect(jsonPath("$.feats[0].feat_id").value(225L))
                .andExpect(jsonPath("$.feats[0].feat_title").value("[UI 컴포넌트] 사용자 입력 폼 개발"))
                .andExpect(jsonPath("$.feats[0].feat_details[0]").value("[UI] 총 예산 상한가 입력 필드 컴포넌트 개발"))
                .andExpect(jsonPath("$.feats[0].priority").doesNotExist());

        verify(projectService).getProject(1L);
        verify(pipelineV3Service).getLatestProjectPipeline(1L, "FE");
        verify(pipelineV3Service, never()).getProjectPipelineSummaries(anyLong());
        verify(projectMemberRepository, never()).findByProjectIdAndUserId(anyLong(), anyLong());
    }

    @Test
    @DisplayName("프로젝트 파이프라인 조회 - FastAPI 404는 생성된 파이프라인 없음 메시지로 반환")
    void getProjectPipelines_NotFoundFromFastApiReturnsEmptyMessage() throws Exception {
        Project project = Project.createProject("Fithub", "desc", 1L);

        when(projectService.getProject(1L)).thenReturn(project);
        when(pipelineV3Service.getLatestProjectPipeline(1L, "FE"))
                .thenThrow(HttpClientErrorException.create(
                        HttpStatus.NOT_FOUND,
                        "Not Found",
                        null,
                        null,
                        null
                ));

        mockMvc.perform(get("/projects/1/pipelines")
                        .param("category", "FE")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("생성된 파이프라인이 없습니다."));

        verify(projectService).getProject(1L);
        verify(pipelineV3Service).getLatestProjectPipeline(1L, "FE");
        verify(projectMemberRepository, never()).findByProjectIdAndUserId(anyLong(), anyLong());
    }
}
