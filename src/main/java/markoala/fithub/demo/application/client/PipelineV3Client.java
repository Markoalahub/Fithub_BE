package markoala.fithub.demo.application.client;

import markoala.fithub.demo.application.dto.response.PipelineListResponse;
import markoala.fithub.demo.application.dto.response.PipelineV3Response;
import markoala.fithub.demo.application.dto.response.PipelineStepV3Response;
import markoala.fithub.demo.application.dto.request.PipelineStepCreateRequest;
import markoala.fithub.demo.application.dto.request.PipelineStepUpdateRequest;
import markoala.fithub.demo.application.dto.request.PipelineV3Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.IOException;

/**
 * FastAPI v3 파이프라인 생성 API 클라이언트.
 */
@Component
public class PipelineV3Client {

    private static final Logger log = LoggerFactory.getLogger(PipelineV3Client.class);
    private final RestClient restClient;

    public PipelineV3Client(@Qualifier("fastApiRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * V3 파이프라인 생성 요청
     */
    public PipelineV3Response generateV3Pipeline(PipelineV3Request request) {
        log.info("[PipelineV3Client] POST /pipelines/generate-v3 | projectId={}, category={}", 
                request.projectId(), request.category());

        MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
        formData.add("project_id", request.projectId());
        formData.add("requirements", request.requirements() != null ? request.requirements() : "");
        
        if (request.category() != null) formData.add("category", request.category());
        if (request.techStack() != null) formData.add("tech_stack", request.techStack());

        if (request.file() != null && !request.file().isEmpty()) {
            try {
                formData.add("file", new ByteArrayResource(request.file().getBytes()) {
                    @Override
                    public String getFilename() {
                        return request.file().getOriginalFilename();
                    }
                });
            } catch (IOException e) {
                log.error("[PipelineV3Client] Failed to read file: {}", e.getMessage());
                throw new RuntimeException("파일 읽기 실패", e);
            }
        }

        return restClient.post()
                .uri("/pipelines/generate-v3")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(formData)
                .retrieve()
                .body(PipelineV3Response.class);
    }

    /**
     * 프로젝트별 파이프라인 목록 조회
     */
    public PipelineListResponse getPipelinesByProject(Long projectId) {
        return restClient.get()
                .uri("/pipelines/project/{projectId}", projectId)
                .retrieve()
                .body(PipelineListResponse.class);
    }

    /**
     * 파이프라인 스텝 추가 (v3 응답 타입 사용)
     */
    public PipelineStepV3Response addPipelineStep(Long pipelineId, PipelineStepCreateRequest request) {
        return restClient.post()
                .uri("/pipelines/{pipelineId}/steps", pipelineId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(PipelineStepV3Response.class);
    }

    /**
     * 파이프라인 스텝 수정 (v3 응답 타입 사용)
     */
    public PipelineStepV3Response updatePipelineStep(Long stepId, PipelineStepUpdateRequest request) {
        return restClient.patch()
                .uri("/pipelines/steps/{stepId}", stepId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(PipelineStepV3Response.class);
    }
    /**
     * 파이프라인 단건 조회
     */
    public PipelineV3Response getPipeline(Long pipelineId) {
        return restClient.get()
                .uri("/pipelines/{pipelineId}", pipelineId)
                .retrieve()
                .body(PipelineV3Response.class);
    }

    /**
     * 파이프라인 삭제
     */
    public void deletePipeline(Long pipelineId) {
        restClient.delete()
                .uri("/pipelines/{pipelineId}", pipelineId)
                .retrieve()
                .toBodilessEntity();
    }

    /**
     * 파이프라인 스텝 삭제
     */
    public void deletePipelineStep(Long stepId) {
        restClient.delete()
                .uri("/pipelines/steps/{stepId}", stepId)
                .retrieve()
                .toBodilessEntity();
    }
}
