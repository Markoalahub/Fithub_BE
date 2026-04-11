package markoala.fithub.demo.application.client;

import markoala.fithub.demo.application.dto.request.PipelineStepCreateRequest;
import markoala.fithub.demo.application.dto.request.PipelineStepUpdateRequest;
import markoala.fithub.demo.application.dto.response.PipelineListResponse;
import markoala.fithub.demo.application.dto.response.PipelineResponse;
import markoala.fithub.demo.application.dto.response.PipelineStepResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
public class PipelineClient {

    private final RestClient restClient;

    public PipelineClient(@Qualifier("fastApiRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public PipelineResponse generateAndSavePipeline(Long projectId, String requirements, String category, byte[] pdfBytes) {
        MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
        formData.add("project_id", projectId.toString());
        formData.add("category", category);
        if (requirements != null && !requirements.isBlank()) {
            formData.add("requirements", requirements);
        }
        if (pdfBytes != null) {
            ByteArrayResource pdfResource = new ByteArrayResource(pdfBytes) {
                @Override
                public String getFilename() { return "prd.pdf"; }
            };
            formData.add("prd_file", pdfResource);
        }

        return restClient.post()
                .uri("/pipelines/generate-and-save")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(formData)
                .retrieve()
                .body(PipelineResponse.class);
    }

    public PipelineListResponse getPipelinesByProject(Long projectId) {
        return getPipelinesByProject(projectId, null);
    }

    public PipelineListResponse getPipelinesByProject(Long projectId, String category) {
        URI uri;
        if (category != null && !category.isBlank()) {
            uri = UriComponentsBuilder.fromPath("/pipelines/project/{projectId}")
                    .queryParam("category", category)
                    .buildAndExpand(projectId)
                    .toUri();
        } else {
            uri = UriComponentsBuilder.fromPath("/pipelines/project/{projectId}")
                    .buildAndExpand(projectId)
                    .toUri();
        }

        return restClient.get()
                .uri(uri)
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

    public void updatePipelineStep(Long stepId, PipelineStepUpdateRequest request) {
        restClient.patch()
                .uri("/pipelines/steps/{stepId}", stepId)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }
}
