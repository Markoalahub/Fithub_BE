package markoala.fithub.demo.application.client;

import markoala.fithub.demo.application.dto.request.PipelineGenerateRequest;
import markoala.fithub.demo.application.dto.request.PipelineStepCreateRequest;
import markoala.fithub.demo.application.dto.request.PipelineStepUpdateRequest;
import markoala.fithub.demo.application.dto.response.PipelineListResponse;
import markoala.fithub.demo.application.dto.response.PipelineResponse;
import markoala.fithub.demo.application.dto.response.PipelineStepResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class PipelineClient {

    private final RestClient restClient;

    public PipelineClient(@Qualifier("fastApiRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public PipelineResponse generateAndSavePipeline(Long projectId, String requirements, String category) {
        PipelineGenerateRequest request = new PipelineGenerateRequest(projectId, requirements, category);

        return restClient.post()
                .uri("/pipelines/generate-and-save")
                .body(request)
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

    public void updatePipelineStep(Long stepId, PipelineStepUpdateRequest request) {
        restClient.patch()
                .uri("/pipelines/steps/{stepId}", stepId)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }
}
