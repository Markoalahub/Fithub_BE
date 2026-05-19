package markoala.fithub.demo.pipeline;

import markoala.fithub.demo.pipeline.dto.PipelineListResponse;
import markoala.fithub.demo.pipeline.dto.PipelineResponse;
import markoala.fithub.demo.pipeline.dto.PipelineStepCreateRequest;
import markoala.fithub.demo.pipeline.dto.PipelineStepResponse;
import markoala.fithub.demo.pipeline.dto.PipelineStepUpdateRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class PipelineClient {

    private final RestClient restClient;

    public PipelineClient(@Qualifier("fastApiRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * FastAPI에 파이프라인 생성 요청 (multipart - PDF 지원)
     */
    public PipelineResponse generateAndSavePipeline(Long projectId, String category, String requirements, byte[] pdfBytes) {
        MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
        formData.add("project_id", projectId);
        formData.add("category", category);
        // requirements가 없으면 빈 문자열 전송 (FastAPI에서 필수)
        formData.add("requirements", requirements != null && !requirements.isBlank() ? requirements : "");
        if (pdfBytes != null && pdfBytes.length > 0) {
            formData.add("prd_file", new ByteArrayResource(pdfBytes) {
                @Override
                public String getFilename() {
                    return "prd.pdf";
                }
            });
        }

        return restClient.post()
                .uri("/pipelines/generate-and-save")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(formData)
                .retrieve()
                .body(PipelineResponse.class);
    }

    /**
     * Stage 1: FastAPI에 유저 플로우 세션 시작 요청 (PDF 지원)
     */
    public Object generateUserFlow(Long projectId, String requirements, String techStack, byte[] pdfBytes) {
        MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
        formData.add("project_id", projectId);
        formData.add("requirements", requirements);
        formData.add("tech_stack", techStack != null ? techStack : "Spring Boot, React");
        if (pdfBytes != null && pdfBytes.length > 0) {
            formData.add("file", new ByteArrayResource(pdfBytes) {
                @Override
                public String getFilename() {
                    return "prd.pdf";
                }
            });
        }

        return restClient.post()
                .uri("/pipelines/generate-userflow")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(formData)
                .retrieve()
                .body(Object.class);
    }

    /**
     * Stage 1 (계속): 기획자 답변 전달 및 확정 요청
     */
    public Object answerUserFlowSession(Long flowId, String answer, Boolean confirm) {
        MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
        formData.add("answer", answer);
        formData.add("confirm", confirm != null ? confirm.toString() : "false");

        return restClient.post()
                .uri("/pipelines/userflow-session/{flowId}/answer", flowId)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(formData)
                .retrieve()
                .body(Object.class);
    }

    /**
     * Stage 2: 유저 플로우 -> ASCII 와이어프레임 생성
     */
    public Object generateWireframe(Long userFlowId) {
        MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
        formData.add("user_flow_id", userFlowId);

        return restClient.post()
                .uri("/pipelines/generate-wireframe")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(formData)
                .retrieve()
                .body(Object.class);
    }

    /**
     * Stage 3: 유저 플로우 + 와이어프레임 -> 개발 파이프라인 생성
     */
    public Object generatePipelineFromFlow(Long userFlowId, Long projectId, String category) {
        MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
        formData.add("user_flow_id", userFlowId);
        formData.add("project_id", projectId);
        formData.add("category", category);

        return restClient.post()
                .uri("/pipelines/generate-pipeline-from-flow")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(formData)
                .retrieve()
                .body(Object.class);
    }

    public PipelineListResponse getPipelinesByProject(Long projectId) {
        return restClient.get()
                .uri("/pipelines/project/{projectId}", projectId)
                .retrieve()
                .body(PipelineListResponse.class);
    }

    public PipelineStepResponse addPipelineStep(Long pipelineId, PipelineStepCreateRequest request) {
        return restClient.post()
                .uri("/pipelines/{pipelineId}/steps", pipelineId)
                .body(request)
                .retrieve()
                .body(PipelineStepResponse.class);
    }

    public PipelineStepResponse updatePipelineStep(Long stepId, PipelineStepUpdateRequest request) {
        return restClient.patch()
                .uri("/pipelines/steps/{stepId}", stepId)
                .body(request)
                .retrieve()
                .body(PipelineStepResponse.class);
    }
}
