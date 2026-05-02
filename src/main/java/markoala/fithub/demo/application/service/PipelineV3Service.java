package markoala.fithub.demo.application.service;

import markoala.fithub.demo.application.client.PipelineV3Client;
import markoala.fithub.demo.application.dto.request.PipelineStepCreateRequest;
import markoala.fithub.demo.application.dto.request.PipelineStepUpdateRequest;
import markoala.fithub.demo.application.dto.response.PipelineListResponse;
import markoala.fithub.demo.application.dto.response.PipelineResponse;
import markoala.fithub.demo.application.dto.response.PipelineStepResponse;
import markoala.fithub.demo.issue.GithubRepository;
import markoala.fithub.demo.issue.RepositoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * v3 파이프라인 생성 서비스 계층.
 *
 * <p>{@link PipelineV3Client}를 통해 FastAPI의 {@code /pipelines/generate-v3}
 * 엔드포인트를 호출하고, 응답 DTO를 그대로 반환하거나 필요한 비즈니스 로직을 수행합니다.</p>
 */
@Service
public class PipelineV3Service {

    private static final Logger log = LoggerFactory.getLogger(PipelineV3Service.class);

    private final PipelineV3Client pipelineV3Client;
    private final RepositoryRepository repositoryRepository;

    public PipelineV3Service(PipelineV3Client pipelineV3Client,
                             RepositoryRepository repositoryRepository) {
        this.pipelineV3Client = pipelineV3Client;
        this.repositoryRepository = repositoryRepository;
    }

    // ─────────────────────────────────────────────────────────────────
    // v3 단일 파이프라인 생성
    // ─────────────────────────────────────────────────────────────────

    /**
     * v3 단일 파이프라인 생성 (MultipartFile 지원).
     *
     * @param projectId    프로젝트 ID (필수)
     * @param requirements 요구사항 텍스트 (필수)
     * @param category     카테고리 (선택, 기본값 "BE")
     * @param prdFile      PRD 파일 (선택)
     * @return FastAPI 응답 PipelineResponse (v3 DTO)
     */
    public PipelineResponse generateV3Pipeline(Long projectId,
                                               String requirements,
                                               String category,
                                               MultipartFile prdFile) {
        log.info("[PipelineV3Service] Generating v3 pipeline — projectId={}, category={}", projectId, category);

        PipelineResponse response = pipelineV3Client.generateV3Pipeline(
                projectId, requirements, category, prdFile);

        log.info("[PipelineV3Service] v3 pipeline generated — id={}, version={}, steps={}",
                response.id(), response.version(),
                response.steps() != null ? response.steps().size() : 0);

        return response;
    }

    // ─────────────────────────────────────────────────────────────────
    // v3 전체 카테고리 파이프라인 일괄 생성
    // ─────────────────────────────────────────────────────────────────

    /**
     * 프로젝트에 등록된 모든 카테고리(FE/BE/AI 등)에 대해 v3 파이프라인 일괄 생성.
     *
     * @param projectId 프로젝트 ID
     * @param prdFile   PRD 파일 (선택)
     * @return 카테고리별로 생성된 PipelineResponse 목록
     */
    public List<PipelineResponse> generateV3PipelinesForAllCategories(Long projectId,
                                                                      MultipartFile prdFile) {
        List<GithubRepository> repos = repositoryRepository.findByProjectId(projectId);

        List<String> categories = repos.stream()
                .map(GithubRepository::getCategory)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .collect(Collectors.toList());

        if (categories.isEmpty()) {
            throw new IllegalArgumentException(
                    "No categorized repositories found for project: " + projectId);
        }

        log.info("[PipelineV3Service] Generating v3 pipelines for project {} — categories: {}", projectId, categories);

        byte[] prdFileBytes = null;
        String filename = null;
        if (prdFile != null && !prdFile.isEmpty()) {
            try {
                prdFileBytes = prdFile.getBytes();
                filename = prdFile.getOriginalFilename();
            } catch (IOException e) {
                log.error("[PipelineV3Service] Failed to read PRD file: {}", e.getMessage());
                throw new RuntimeException("PRD 파일 읽기 실패", e);
            }
        }

        final byte[] finalPrdBytes = prdFileBytes;
        final String finalFilename = filename;

        return categories.stream()
                .map(category -> {
                    log.info("[PipelineV3Service] Generating v3 pipeline for category: {}", category);
                    return pipelineV3Client.generateV3Pipeline(
                            projectId, "", category, finalPrdBytes, finalFilename);
                })
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────
    // 조회 / 스텝 추가 / 스텝 수정
    // ─────────────────────────────────────────────────────────────────

    /**
     * 프로젝트별 파이프라인 목록 조회.
     */
    public PipelineListResponse getPipelinesByProject(Long projectId) {
        log.info("[PipelineV3Service] Fetching pipelines for project {}", projectId);
        return pipelineV3Client.getPipelinesByProject(projectId);
    }

    /**
     * 파이프라인에 스텝 추가.
     */
    public PipelineStepResponse addStepToPipeline(Long pipelineId, PipelineStepCreateRequest request) {
        log.info("[PipelineV3Service] Adding step to pipeline {}", pipelineId);
        return pipelineV3Client.addPipelineStep(pipelineId, request);
    }

    /**
     * 파이프라인 스텝 수정.
     */
    public PipelineStepResponse updatePipelineStep(Long stepId, PipelineStepUpdateRequest request) {
        log.info("[PipelineV3Service] Updating pipeline step {}", stepId);
        return pipelineV3Client.updatePipelineStep(stepId, request);
    }
}
