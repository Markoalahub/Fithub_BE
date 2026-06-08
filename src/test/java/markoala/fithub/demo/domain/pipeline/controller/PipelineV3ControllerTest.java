package markoala.fithub.demo.domain.pipeline.controller;

import markoala.fithub.demo.domain.pipeline.dto.request.PipelineGithubRepositoryUpdateRequest;
import markoala.fithub.demo.domain.pipeline.dto.response.FeatResponse;
import markoala.fithub.demo.domain.pipeline.dto.response.PipelineListResponse;
import markoala.fithub.demo.domain.pipeline.dto.response.PipelineV3Response;
import markoala.fithub.demo.domain.pipeline.service.PipelineV3Service;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PipelineV3Controller.class)
@Import(SecurityConfig.class)
public class PipelineV3ControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
    @DisplayName("파이프라인 생성 성공 (PDF 업로드 포함)")
    void generateV3Pipeline_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "dummy content".getBytes()
        );

        when(pipelineV3Service.generateV3Pipeline(eq(1L), any()))
                .thenReturn(new PipelineV3Response(
                        1L,
                        1L,
                        "BE",
                        1,
                        "Spring Boot",
                        List.of(new FeatResponse(
                                100L,
                                "로그인 API 구현",
                                List.of("JWT 기반 로그인 API를 구현합니다."),
                                1
                        ))
                ));

        mockMvc.perform(multipart("/pipelines/generate")
                        .file(file)
                        .param("project_id", "1")
                        .param("requirements", "Make a login feature")
                        .param("category", "BE")
                        
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pipe_id").value(1))
                .andExpect(jsonPath("$.feats[0].feat_id").value(100L))
                .andExpect(jsonPath("$.feats[0].priority").doesNotExist());

        verify(pipelineV3Service).generateV3Pipeline(eq(1L), argThat(request ->
                request != null
                        && request.projectId().equals(1L)
                        && "BE".equals(request.category())
        ));
    }

    @Test
    @DisplayName("ALL 파이프라인 생성 요청은 목록 응답을 반환한다")
    void generateV3Pipeline_AllCategoryReturnsList() throws Exception {
        PipelineListResponse response = new PipelineListResponse(
                List.of(
                        new PipelineV3Response(10L, 1L, "BE", 1, "Spring Boot, React", Collections.emptyList()),
                        new PipelineV3Response(
                                11L,
                                1L,
                                "FE",
                                1,
                                "Spring Boot, React",
                                List.of(new FeatResponse(
                                        101L,
                                        "회원가입 화면 구현",
                                        List.of("입력 폼을 구현합니다."),
                                        2
                                ))
                        )
                ),
                2L
        );

        when(pipelineV3Service.generateV3Pipeline(eq(1L), any())).thenReturn(response);

        mockMvc.perform(multipart("/pipelines/generate")
                        .param("project_id", "1")
                        .param("requirements", "Make a login feature")
                        .param("category", "ALL")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2L))
                .andExpect(jsonPath("$.pipelines[0].pipe_id").value(10L))
                .andExpect(jsonPath("$.pipelines[0].category").value("BE"))
                .andExpect(jsonPath("$.pipelines[1].pipe_id").value(11L))
                .andExpect(jsonPath("$.pipelines[1].category").value("FE"))
                .andExpect(jsonPath("$.pipelines[1].feats[0].feat_id").value(101L))
                .andExpect(jsonPath("$.pipelines[1].feats[0].priority").doesNotExist());

        verify(pipelineV3Service).generateV3Pipeline(eq(1L), argThat(request ->
                request != null
                        && request.projectId().equals(1L)
                        && "ALL".equals(request.category())
        ));
    }

    @Test
    @DisplayName("파이프라인 생성 가능 횟수를 모두 사용하면 429를 반환한다")
    void generateV3Pipeline_QuotaExceeded() throws Exception {
        when(pipelineV3Service.generateV3Pipeline(eq(1L), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "파이프라인 생성 가능 횟수를 모두 사용했습니다."));

        mockMvc.perform(multipart("/pipelines/generate")
                        .param("project_id", "1")
                        .param("requirements", "Make a login feature")
                        .param("category", "BE")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("파이프라인 생성 가능 횟수를 모두 사용했습니다."));

        verify(pipelineV3Service).generateV3Pipeline(eq(1L), argThat(request ->
                request != null && "BE".equals(request.category())
        ));
    }

    @Test
    @DisplayName("프로젝트 멤버가 아니면 파이프라인 생성 요청은 403을 반환한다")
    void generateV3Pipeline_NotProjectMember() throws Exception {
        when(pipelineV3Service.generateV3Pipeline(eq(1L), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "해당 프로젝트에 속한 사용자만 파이프라인을 생성할 수 있습니다."));

        mockMvc.perform(multipart("/pipelines/generate")
                        .param("project_id", "1")
                        .param("requirements", "Make a login feature")
                        .param("category", "BE")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("해당 프로젝트에 속한 사용자만 파이프라인을 생성할 수 있습니다."));

        verify(pipelineV3Service).generateV3Pipeline(eq(1L), argThat(request ->
                request != null
                        && request.projectId().equals(1L)
                        && "BE".equals(request.category())
        ));
    }

    @Test
    @DisplayName("파이프라인 생성 실패 - PDF가 아닌 파일 (400)")
    void generateV3Pipeline_NotPdfFail() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.png", "image/png", "dummy content".getBytes()
        );

        mockMvc.perform(multipart("/pipelines/generate")
                        .file(file)
                        .param("project_id", "1")
                        .param("requirements", "Make a login feature")
                        .param("category", "BE")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("파이프라인 생성 실패 - 필수 파라미터 누락 (400)")
    void generateV3Pipeline_MissingParamFail() throws Exception {
        mockMvc.perform(multipart("/pipelines/generate")
                        .param("requirements", "Missing project_id")
                        .param("category", "BE")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("파이프라인 GitHub repository URL 연결 성공")
    void updatePipelineGithubRepository_Success() throws Exception {
        PipelineV3Response response = new PipelineV3Response(
                33L,
                1L,
                "BE",
                1,
                "Spring Boot",
                "https://github.com/Markoalahub/Fithub_BE",
                List.of(new FeatResponse(
                        200L,
                        "로그인 API 구현",
                        List.of("JWT 기반 로그인 API를 구현합니다."),
                        1
                ))
        );

        when(pipelineV3Service.updatePipelineGithubRepository(eq(33L), any(PipelineGithubRepositoryUpdateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(patch("/pipelines/33/github")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"github_repo_url\":\"https://github.com/Markoalahub/Fithub_BE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pipe_id").value(33L))
                .andExpect(jsonPath("$.project_id").value(1L))
                .andExpect(jsonPath("$.category").value("BE"))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.tech_stack").value("Spring Boot"))
                .andExpect(jsonPath("$.github_repo_url").value("https://github.com/Markoalahub/Fithub_BE"))
                .andExpect(jsonPath("$.feats").doesNotExist());

        verify(pipelineV3Service).updatePipelineGithubRepository(eq(33L), argThat(request ->
                request != null && "https://github.com/Markoalahub/Fithub_BE".equals(request.githubRepoUrl())
        ));
    }

    @Test
    @DisplayName("파이프라인 GitHub repository URL 연결 실패 - URL 형식 오류 (400)")
    void updatePipelineGithubRepository_InvalidUrl() throws Exception {
        mockMvc.perform(patch("/pipelines/33/github-repository")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"github_repo_url\":\"git@github.com:Markoalahub/Fithub_BE.git\"}"))
                .andExpect(status().isBadRequest());
    }
}
