package markoala.fithub.demo.domain.pipeline.controller;

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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
        when(jwtProvider.validateToken(anyString())).thenReturn(true);
        when(jwtProvider.getUserIdFromToken(anyString())).thenReturn(1L);
    }

    @Test
    @DisplayName("파이프라인 생성 성공 (PDF 업로드 포함)")
    void generateV3Pipeline_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "dummy content".getBytes()
        );

        when(pipelineV3Service.generateV3Pipeline(any()))
                .thenReturn(new PipelineV3Response(1L, 1L, "category", 1, "status", Collections.emptyList()));

        mockMvc.perform(multipart("/pipelines/generate")
                        .file(file)
                        .param("project_id", "1")
                        .param("requirements", "Make a login feature")
                        .param("category", "BE")
                        
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pipe_id").value(1));
    }

    @Test
    @DisplayName("ALL 파이프라인 생성 요청은 목록 응답을 반환한다")
    void generateV3Pipeline_AllCategoryReturnsList() throws Exception {
        PipelineListResponse response = new PipelineListResponse(
                List.of(
                        new PipelineV3Response(10L, 1L, "BE", 1, "Spring Boot, React", Collections.emptyList()),
                        new PipelineV3Response(11L, 1L, "FE", 1, "Spring Boot, React", Collections.emptyList())
                ),
                2L
        );

        when(pipelineV3Service.generateV3Pipeline(any())).thenReturn(response);

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
                .andExpect(jsonPath("$.pipelines[1].category").value("FE"));

        verify(pipelineV3Service).generateV3Pipeline(argThat(request ->
                request != null && "ALL".equals(request.category())
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
}
