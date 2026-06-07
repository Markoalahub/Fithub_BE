package markoala.fithub.demo.domain.issue;

import markoala.fithub.demo.domain.issue.dto.RepositoryIssueCreateRequest;
import markoala.fithub.demo.domain.issue.dto.RepositoryIssueCreateResponse;
import markoala.fithub.demo.global.config.SecurityConfig;
import markoala.fithub.demo.global.security.jwt.JwtProvider;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PipelineIssueController.class)
@Import(SecurityConfig.class)
class PipelineIssueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RepositoryIssueService repositoryIssueService;

    @MockBean
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        when(jwtProvider.validateToken(anyString())).thenReturn(true);
        when(jwtProvider.getUserIdFromToken(anyString())).thenReturn(1L);
    }

    @Test
    @DisplayName("파이프라인에 연결된 레포지토리에 feat_detail을 제목으로 GitHub 이슈를 생성한다")
    void createPipelineIssue_UsesCurrentUserIdAndPipelineId() throws Exception {
        when(repositoryIssueService.createIssue(eq(1L), eq(33L), any(RepositoryIssueCreateRequest.class)))
                .thenReturn(new RepositoryIssueCreateResponse(
                        10L,
                        33L,
                        42,
                        "https://github.com/Markoalahub/Fithub_BE/issues/42",
                        "로그인 API 구현",
                        "JWT 기반 로그인 API를 구현합니다.",
                        "open"
                ));

        mockMvc.perform(post("/pipelines/33/issues")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "feat_detail": "로그인 API 구현",
                                  "body": "JWT 기반 로그인 API를 구현합니다."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.repo_id").value(10L))
                .andExpect(jsonPath("$.pipeline_id").value(33L))
                .andExpect(jsonPath("$.github_issue_number").value(42))
                .andExpect(jsonPath("$.github_issue_url").value("https://github.com/Markoalahub/Fithub_BE/issues/42"))
                .andExpect(jsonPath("$.title").value("로그인 API 구현"))
                .andExpect(jsonPath("$.body").value("JWT 기반 로그인 API를 구현합니다."))
                .andExpect(jsonPath("$.state").value("open"));

        verify(repositoryIssueService).createIssue(
                eq(1L),
                eq(33L),
                argThat(request ->
                        request != null
                                && request.featDetail().equals("로그인 API 구현")
                                && request.body().equals("JWT 기반 로그인 API를 구현합니다.")
                )
        );
    }

    @Test
    @DisplayName("feat_detail 없이 GitHub 이슈 생성을 요청하면 400을 반환한다")
    void createPipelineIssue_ReturnsBadRequestWhenFeatDetailMissing() throws Exception {
        mockMvc.perform(post("/pipelines/33/issues")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "body": "JWT 기반 로그인 API를 구현합니다."
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("파이프라인이 없으면 명확한 404 메시지를 반환한다")
    void createPipelineIssue_ReturnsPipelineNotFoundMessage() throws Exception {
        when(repositoryIssueService.createIssue(eq(1L), eq(99L), any(RepositoryIssueCreateRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "파이프라인을 찾을 수 없습니다."));

        mockMvc.perform(post("/pipelines/99/issues")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "feat_detail": "로그인 API 구현",
                                  "body": "JWT 기반 로그인 API를 구현합니다."
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("파이프라인을 찾을 수 없습니다."));

        verify(repositoryIssueService).createIssue(eq(1L), eq(99L), any(RepositoryIssueCreateRequest.class));
    }

    @Test
    @DisplayName("파이프라인에 연결된 레포 URL이 없으면 명확한 400 메시지를 반환한다")
    void createPipelineIssue_ReturnsPipelineRepositoryUrlMissingMessage() throws Exception {
        when(repositoryIssueService.createIssue(eq(1L), eq(33L), any(RepositoryIssueCreateRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "파이프라인에 연결된 GitHub repository URL이 없습니다. 먼저 레포지토리를 연결해주세요."));

        mockMvc.perform(post("/pipelines/33/issues")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "feat_detail": "로그인 API 구현",
                                  "body": "JWT 기반 로그인 API를 구현합니다."
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("파이프라인에 연결된 GitHub repository URL이 없습니다. 먼저 레포지토리를 연결해주세요."));

        verify(repositoryIssueService).createIssue(eq(1L), eq(33L), any(RepositoryIssueCreateRequest.class));
    }
}
