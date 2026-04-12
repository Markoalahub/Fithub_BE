package markoala.fithub.demo.pipeline;

import markoala.fithub.demo.pipeline.dto.PipelineGenerateRequest;
import markoala.fithub.demo.pipeline.dto.PipelineListResponse;
import markoala.fithub.demo.pipeline.dto.PipelineResponse;
import markoala.fithub.demo.pipeline.dto.PipelineStepCreateRequest;
import markoala.fithub.demo.pipeline.dto.PipelineStepResponse;
import markoala.fithub.demo.pipeline.dto.PipelineStepUpdateRequest;
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
