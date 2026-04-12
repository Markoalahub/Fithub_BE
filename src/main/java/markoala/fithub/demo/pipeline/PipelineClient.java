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
        if (requirements != null && !requirements.isBlank()) {
            formData.add("requirements", requirements);
        }
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
