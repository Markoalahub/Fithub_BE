package markoala.fithub.demo.domain.pipeline.service;

import markoala.fithub.demo.domain.issue.GitHubIssueService;
import markoala.fithub.demo.domain.issue.IssueRepository;
import markoala.fithub.demo.domain.issue.RepositoryRepository;
import markoala.fithub.demo.domain.pipeline.client.PipelineV3Client;
import markoala.fithub.demo.domain.pipeline.dto.request.PipelineGithubRepositoryUpdateRequest;
import markoala.fithub.demo.domain.pipeline.dto.request.PipelineV3Request;
import markoala.fithub.demo.domain.pipeline.dto.response.PipelineListResponse;
import markoala.fithub.demo.domain.pipeline.dto.response.PipelineSummaryResponse;
import markoala.fithub.demo.domain.pipeline.dto.response.PipelineV3Response;
import markoala.fithub.demo.domain.pipeline.dto.response.ProjectPipelineSummaryListResponse;
import markoala.fithub.demo.domain.project.Project;
import markoala.fithub.demo.domain.project.ProjectMember;
import markoala.fithub.demo.domain.project.ProjectMemberRepository;
import markoala.fithub.demo.domain.project.ProjectRepository;
import markoala.fithub.demo.domain.user.UserService;
import markoala.fithub.demo.global.security.jwt.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PipelineV3ServiceTest {

    @Mock
    private PipelineV3Client pipelineV3Client;

    @Mock
    private RepositoryRepository repositoryRepository;

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private GitHubIssueService gitHubIssueService;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private UserService userService;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @InjectMocks
    private PipelineV3Service pipelineV3Service;

    @Test
    @DisplayName("프로젝트 파이프라인 요약 목록 조회는 클라이언트 요약 조회를 호출한다")
    void getProjectPipelineSummaries_CallsClient() {
        ProjectPipelineSummaryListResponse expected = new ProjectPipelineSummaryListResponse(
                1L,
                List.of(new PipelineSummaryResponse(33L, "FE 파이프라인 33", "FE")),
                1L
        );
        when(pipelineV3Client.getProjectPipelineSummaries(1L)).thenReturn(expected);

        ProjectPipelineSummaryListResponse actual = pipelineV3Service.getProjectPipelineSummaries(1L);

        assertThat(actual).isSameAs(expected);
        verify(pipelineV3Client).getProjectPipelineSummaries(1L);
    }

    @Test
    @DisplayName("프로젝트 카테고리 최신 파이프라인 조회는 클라이언트 최신 조회를 호출한다")
    void getLatestProjectPipeline_CallsClient() {
        PipelineV3Response expected = new PipelineV3Response(
                33L,
                1L,
                "FE",
                15,
                "React, expo",
                List.of()
        );
        when(pipelineV3Client.getLatestProjectPipeline(1L, "FE")).thenReturn(expected);

        PipelineV3Response actual = pipelineV3Service.getLatestProjectPipeline(1L, "FE");

        assertThat(actual).isSameAs(expected);
        verify(pipelineV3Client).getLatestProjectPipeline(1L, "FE");
    }

    @Test
    @DisplayName("ALL 카테고리 생성은 BE와 FE 파이프라인 생성 호출로 분리한다")
    void generateV3Pipeline_AllCategory_CallsBeAndFeGeneration() {
        PipelineV3Request request = new PipelineV3Request(
                1L,
                "requirements",
                "ALL",
                "Spring Boot, React",
                null
        );
        PipelineV3Response beResponse = new PipelineV3Response(
                10L,
                1L,
                "BE",
                1,
                "Spring Boot, React",
                List.of()
        );
        PipelineV3Response feResponse = new PipelineV3Response(
                11L,
                1L,
                "FE",
                1,
                "Spring Boot, React",
                List.of()
        );

        when(projectRepository.findById(1L)).thenReturn(Optional.of(Project.createProject("Fithub", "desc", 1L)));
        when(projectMemberRepository.findByProjectIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(ProjectMember.createMember(1L, 1L, "PLANNER")));
        when(pipelineV3Client.generateV3Pipeline(argThat(req -> req != null && "BE".equals(req.category()))))
                .thenReturn(beResponse);
        when(pipelineV3Client.generateV3Pipeline(argThat(req -> req != null && "FE".equals(req.category()))))
                .thenReturn(feResponse);

        Object actual = pipelineV3Service.generateV3Pipeline(1L, request);

        assertThat(actual).isInstanceOf(PipelineListResponse.class);
        PipelineListResponse response = (PipelineListResponse) actual;
        assertThat(response.total()).isEqualTo(2L);
        assertThat(response.pipelines()).extracting(PipelineV3Response::category)
                .containsExactly("BE", "FE");

        verify(projectRepository).findById(1L);
        verify(projectMemberRepository).findByProjectIdAndUserId(1L, 1L);
        verify(userService).consumeAiPipelineGenerationQuota(1L, 2);
        verify(userService, never()).restoreAiPipelineGenerationQuota(anyLong());
        verify(pipelineV3Client).generateV3Pipeline(argThat(req -> req != null && "BE".equals(req.category())));
        verify(pipelineV3Client).generateV3Pipeline(argThat(req -> req != null && "FE".equals(req.category())));
        verify(pipelineV3Client, never()).generateV3Pipeline(argThat(req -> req != null && "ALL".equals(req.category())));
    }

    @Test
    @DisplayName("파이프라인 생성 한도를 모두 사용하면 FastAPI를 호출하지 않는다")
    void generateV3Pipeline_QuotaExceeded_DoesNotCallClient() {
        PipelineV3Request request = new PipelineV3Request(
                1L,
                "requirements",
                "BE",
                "Spring Boot",
                null
        );

        when(projectRepository.findById(1L)).thenReturn(Optional.of(Project.createProject("Fithub", "desc", 1L)));
        when(projectMemberRepository.findByProjectIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(ProjectMember.createMember(1L, 1L, "PLANNER")));
        doThrow(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "파이프라인 생성 가능 횟수를 모두 사용했습니다."))
                .when(userService).consumeAiPipelineGenerationQuota(1L, 1);

        assertThatThrownBy(() -> pipelineV3Service.generateV3Pipeline(1L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("파이프라인 생성 가능 횟수를 모두 사용했습니다.");

        verify(projectRepository).findById(1L);
        verify(projectMemberRepository).findByProjectIdAndUserId(1L, 1L);
        verify(userService).consumeAiPipelineGenerationQuota(1L, 1);
        verify(userService, never()).restoreAiPipelineGenerationQuota(anyLong());
        verifyNoInteractions(pipelineV3Client);
    }

    @Test
    @DisplayName("ALL 파이프라인 생성에 필요한 생성 횟수가 부족하면 FastAPI를 호출하지 않는다")
    void generateV3Pipeline_AllCategoryQuotaExceeded_DoesNotCallClient() {
        PipelineV3Request request = new PipelineV3Request(
                1L,
                "requirements",
                "ALL",
                "Spring Boot, React",
                null
        );

        when(projectRepository.findById(1L)).thenReturn(Optional.of(Project.createProject("Fithub", "desc", 1L)));
        when(projectMemberRepository.findByProjectIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(ProjectMember.createMember(1L, 1L, "PLANNER")));
        doThrow(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "파이프라인 생성 가능 횟수를 모두 사용했습니다."))
                .when(userService).consumeAiPipelineGenerationQuota(1L, 2);

        assertThatThrownBy(() -> pipelineV3Service.generateV3Pipeline(1L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("파이프라인 생성 가능 횟수를 모두 사용했습니다.");

        verify(projectRepository).findById(1L);
        verify(projectMemberRepository).findByProjectIdAndUserId(1L, 1L);
        verify(userService).consumeAiPipelineGenerationQuota(1L, 2);
        verify(userService, never()).restoreAiPipelineGenerationQuota(anyLong(), anyInt());
        verifyNoInteractions(pipelineV3Client);
    }

    @Test
    @DisplayName("FastAPI 생성 호출이 실패하면 차감한 생성 횟수를 복구한다")
    void generateV3Pipeline_RestoresQuotaWhenClientFails() {
        PipelineV3Request request = new PipelineV3Request(
                1L,
                "requirements",
                "BE",
                "Spring Boot",
                null
        );

        when(projectRepository.findById(1L)).thenReturn(Optional.of(Project.createProject("Fithub", "desc", 1L)));
        when(projectMemberRepository.findByProjectIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(ProjectMember.createMember(1L, 1L, "PLANNER")));
        when(pipelineV3Client.generateV3Pipeline(request)).thenThrow(new RuntimeException("FastAPI error"));

        assertThatThrownBy(() -> pipelineV3Service.generateV3Pipeline(1L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("FastAPI error");

        verify(projectRepository).findById(1L);
        verify(projectMemberRepository).findByProjectIdAndUserId(1L, 1L);
        verify(userService).consumeAiPipelineGenerationQuota(1L, 1);
        verify(userService).restoreAiPipelineGenerationQuota(1L, 1);
        verify(pipelineV3Client).generateV3Pipeline(request);
    }

    @Test
    @DisplayName("프로젝트 멤버가 아니면 파이프라인 생성 횟수를 차감하지 않고 403을 반환한다")
    void generateV3Pipeline_NotProjectMember_ForbiddenBeforeQuota() {
        PipelineV3Request request = new PipelineV3Request(
                1L,
                "requirements",
                "BE",
                "Spring Boot",
                null
        );

        when(projectRepository.findById(1L)).thenReturn(Optional.of(Project.createProject("Fithub", "desc", 2L)));
        when(projectMemberRepository.findByProjectIdAndUserId(1L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pipelineV3Service.generateV3Pipeline(1L, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException responseStatusException = (ResponseStatusException) ex;
                    assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(responseStatusException.getReason()).isEqualTo("해당 프로젝트에 속한 사용자만 파이프라인을 생성할 수 있습니다.");
                });

        verify(projectRepository).findById(1L);
        verify(projectMemberRepository).findByProjectIdAndUserId(1L, 1L);
        verify(userService, never()).consumeAiPipelineGenerationQuota(anyLong());
        verify(userService, never()).restoreAiPipelineGenerationQuota(anyLong());
        verifyNoInteractions(pipelineV3Client);
    }

    @Test
    @DisplayName("인증 사용자 ID가 없으면 파이프라인 생성 횟수를 차감하지 않고 401을 반환한다")
    void generateV3Pipeline_Unauthenticated_UnauthorizedBeforeQuota() {
        PipelineV3Request request = new PipelineV3Request(
                1L,
                "requirements",
                "BE",
                "Spring Boot",
                null
        );

        when(projectRepository.findById(1L)).thenReturn(Optional.of(Project.createProject("Fithub", "desc", 1L)));

        assertThatThrownBy(() -> pipelineV3Service.generateV3Pipeline(null, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException responseStatusException = (ResponseStatusException) ex;
                    assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(responseStatusException.getReason()).isEqualTo("인증이 필요합니다.");
                });

        verify(projectRepository).findById(1L);
        verify(projectMemberRepository, never()).findByProjectIdAndUserId(anyLong(), anyLong());
        verify(userService, never()).consumeAiPipelineGenerationQuota(anyLong());
        verify(userService, never()).restoreAiPipelineGenerationQuota(anyLong());
        verifyNoInteractions(pipelineV3Client);
    }

    @Test
    @DisplayName("파이프라인 GitHub repository URL 연결은 클라이언트 연결 API를 호출한다")
    void updatePipelineGithubRepository_CallsClient() {
        PipelineGithubRepositoryUpdateRequest request = new PipelineGithubRepositoryUpdateRequest(
                "https://github.com/Markoalahub/Fithub_BE"
        );
        PipelineV3Response expected = new PipelineV3Response(
                33L,
                1L,
                "BE",
                1,
                "Spring Boot",
                "https://github.com/Markoalahub/Fithub_BE",
                List.of()
        );

        when(pipelineV3Client.updatePipelineGithubRepository(33L, request)).thenReturn(expected);

        PipelineV3Response actual = pipelineV3Service.updatePipelineGithubRepository(33L, request);

        assertThat(actual).isSameAs(expected);
        verify(pipelineV3Client).updatePipelineGithubRepository(33L, request);
    }
}
