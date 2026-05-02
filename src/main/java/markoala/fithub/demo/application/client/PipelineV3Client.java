package markoala.fithub.demo.application.client;

import markoala.fithub.demo.application.dto.response.PipelineListResponse;
import markoala.fithub.demo.application.dto.response.PipelineResponse;
import markoala.fithub.demo.application.dto.response.PipelineStepResponse;
import markoala.fithub.demo.application.dto.request.PipelineStepCreateRequest;
import markoala.fithub.demo.application.dto.request.PipelineStepUpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * FastAPI v3 파이프라인 생성 API 클라이언트.
 *
 * <p>호출 대상 엔드포인트: {@code POST {fastapi-url}/pipelines/generate-v3}</p>
 * <p>Content-Type: {@code multipart/form-data}</p>
 *
 * <h3>Request Form Data</h3>
 * <ul>
 *   <li>{@code project_id} (Integer) — 필수</li>
 *   <li>{@code requirements} (String) — 필수</li>
 *   <li>{@code category} (String) — 선택 (기본값 "BE")</li>
 *   <li>{@code prd_file} (File) — 선택</li>
 * </ul>
 */
@Component
public class PipelineV3Client {

    private static final Logger log = LoggerFactory.getLogger(PipelineV3Client.class);

    private final RestClient restClient;

    public PipelineV3Client(@Qualifier("fastApiRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    // ─────────────────────────────────────────────────────────────────
    // v3 파이프라인 생성
    // ─────────────────────────────────────────────────────────────────

    /**
     * FastAPI v3 파이프라인 생성 요청 (multipart/form-data).
     *
     * @param projectId    프로젝트 ID (필수)
     * @param requirements 요구사항 텍스트 (필수)
     * @param category     카테고리 (선택, 기본값 "BE")
     * @param prdFile      PRD 파일 (선택, null 허용)
     * @return FastAPI가 반환한 PipelineResponse (v3 DTO)
     */
    public PipelineResponse generateV3Pipeline(Long projectId,
                                               String requirements,
                                               String category,
                                               MultipartFile prdFile) {
        log.info("[PipelineV3Client] Calling /pipelines/generate-v3  projectId={}, category={}", projectId, category);

        MultiValueMap<String, Object> formData = buildFormData(projectId, requirements, category, prdFile);

        PipelineResponse response = restClient.post()
                .uri("/pipelines/generate-v3")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(formData)
                .retrieve()
                .body(PipelineResponse.class);

        log.info("[PipelineV3Client] Pipeline created — id={}, steps={}",
                response != null ? response.id() : "null",
                response != null && response.steps() != null ? response.steps().size() : 0);

        return response;
    }

    /**
     * 바이트 배열(PDF 등)로 PRD를 전달하는 오버로드 버전.
     */
    public PipelineResponse generateV3Pipeline(Long projectId,
                                               String requirements,
                                               String category,
                                               byte[] prdFileBytes,
                                               String originalFilename) {
        log.info("[PipelineV3Client] Calling /pipelines/generate-v3 (byte[])  projectId={}, category={}", projectId, category);

        MultiValueMap<String, Object> formData = buildFormDataFromBytes(projectId, requirements, category, prdFileBytes, originalFilename);

        PipelineResponse response = restClient.post()
                .uri("/pipelines/generate-v3")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(formData)
                .retrieve()
                .body(PipelineResponse.class);

        log.info("[PipelineV3Client] Pipeline created — id={}, steps={}",
                response != null ? response.id() : "null",
                response != null && response.steps() != null ? response.steps().size() : 0);

        return response;
    }

    // ─────────────────────────────────────────────────────────────────
    // 기타 파이프라인 API 호출
    // ─────────────────────────────────────────────────────────────────

    /**
     * 프로젝트별 파이프라인 목록 조회.
     */
    public PipelineListResponse getPipelinesByProject(Long projectId) {
        log.debug("[PipelineV3Client] GET /pipelines/project/{}", projectId);
        return restClient.get()
                .uri("/pipelines/project/{projectId}", projectId)
                .retrieve()
                .body(PipelineListResponse.class);
    }

    /**
     * 파이프라인에 스텝 추가.
     */
    public PipelineStepResponse addPipelineStep(Long pipelineId, PipelineStepCreateRequest request) {
        log.debug("[PipelineV3Client] POST /pipelines/{}/steps", pipelineId);
        return restClient.post()
                .uri("/pipelines/{pipelineId}/steps", pipelineId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(PipelineStepResponse.class);
    }

    /**
     * 파이프라인 스텝 수정.
     */
    public PipelineStepResponse updatePipelineStep(Long stepId, PipelineStepUpdateRequest request) {
        log.debug("[PipelineV3Client] PATCH /pipelines/steps/{}", stepId);
        return restClient.patch()
                .uri("/pipelines/steps/{stepId}", stepId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(PipelineStepResponse.class);
    }

    // ─────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────

    /**
     * MultipartFile → multipart/form-data body 변환.
     */
    private MultiValueMap<String, Object> buildFormData(Long projectId,
                                                        String requirements,
                                                        String category,
                                                        MultipartFile prdFile) {
        MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();

        // 필수 파라미터
        formData.add("project_id", projectId);
        formData.add("requirements", requirements != null ? requirements : "");

        // 선택 파라미터
        if (category != null && !category.isBlank()) {
            formData.add("category", category);
        }

        // 파일 (선택)
        if (prdFile != null && !prdFile.isEmpty()) {
            try {
                byte[] fileBytes = prdFile.getBytes();
                String filename = prdFile.getOriginalFilename() != null
                        ? prdFile.getOriginalFilename()
                        : "prd_file";

                formData.add("prd_file", new ByteArrayResource(fileBytes) {
                    @Override
                    public String getFilename() {
                        return filename;
                    }
                });
            } catch (IOException e) {
                log.error("[PipelineV3Client] Failed to read prd_file bytes: {}", e.getMessage());
                throw new RuntimeException("PRD 파일 읽기 실패", e);
            }
        }

        return formData;
    }

    /**
     * byte[] → multipart/form-data body 변환.
     */
    private MultiValueMap<String, Object> buildFormDataFromBytes(Long projectId,
                                                                 String requirements,
                                                                 String category,
                                                                 byte[] prdFileBytes,
                                                                 String originalFilename) {
        MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();

        // 필수 파라미터
        formData.add("project_id", projectId);
        formData.add("requirements", requirements != null ? requirements : "");

        // 선택 파라미터
        if (category != null && !category.isBlank()) {
            formData.add("category", category);
        }

        // 파일 (선택)
        if (prdFileBytes != null && prdFileBytes.length > 0) {
            String filename = (originalFilename != null && !originalFilename.isBlank())
                    ? originalFilename
                    : "prd.pdf";

            formData.add("prd_file", new ByteArrayResource(prdFileBytes) {
                @Override
                public String getFilename() {
                    return filename;
                }
            });
        }

        return formData;
    }
}
