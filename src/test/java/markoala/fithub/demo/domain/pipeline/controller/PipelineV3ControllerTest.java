package markoala.fithub.demo.domain.pipeline.controller;

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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
    void generateAllV3Pipelines_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "dummy content".getBytes()
        );

        when(pipelineV3Service.generateV3PipelinesForCategories(any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(multipart("/pipelines/generate-all")
                        .file(file)
                        .param("project_id", "1")
                        .param("requirements", "Make a login feature")
                        .param("categories", "BE")
                        .param("categories", "FE")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("파이프라인 생성 실패 - PDF가 아닌 파일 (400)")
    void generateAllV3Pipelines_NotPdfFail() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.png", "image/png", "dummy content".getBytes()
        );

        mockMvc.perform(multipart("/pipelines/generate-all")
                        .file(file)
                        .param("project_id", "1")
                        .param("requirements", "Make a login feature")
                        .param("categories", "BE")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("파이프라인 생성 실패 - 필수 파라미터 누락 (400)")
    void generateAllV3Pipelines_MissingParamFail() throws Exception {
        mockMvc.perform(multipart("/pipelines/generate-all")
                        .param("requirements", "Missing project_id")
                        .param("categories", "BE")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isBadRequest());
    }
}
