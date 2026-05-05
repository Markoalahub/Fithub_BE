package markoala.fithub.demo.application.service;

import markoala.fithub.demo.application.client.PipelineV3Client;

import markoala.fithub.demo.application.dto.request.PipelineStepCreateRequest;
import markoala.fithub.demo.application.dto.request.PipelineStepUpdateRequest;
import markoala.fithub.demo.application.dto.request.PipelineV3Request;
import markoala.fithub.demo.application.dto.response.PipelineListResponse;
import markoala.fithub.demo.application.dto.response.PipelineV3Response;
import markoala.fithub.demo.application.dto.response.PipelineStepV3Response;
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
    private final markoala.fithub.demo.issue.IssueRepository issueRepository;
    private final markoala.fithub.demo.issue.GitHubIssueService gitHubIssueService;
    private final markoala.fithub.demo.global.security.jwt.JwtProvider jwtProvider;
    private final markoala.fithub.demo.user.UserService userService;
    private final markoala.fithub.demo.project.ProjectRepository projectRepository;

    public PipelineV3Service(PipelineV3Client pipelineV3Client,
                             RepositoryRepository repositoryRepository,
                             markoala.fithub.demo.issue.IssueRepository issueRepository,
                             markoala.fithub.demo.issue.GitHubIssueService gitHubIssueService,
                             markoala.fithub.demo.global.security.jwt.JwtProvider jwtProvider,
                             markoala.fithub.demo.user.UserService userService,
                             markoala.fithub.demo.project.ProjectRepository projectRepository) {
        this.pipelineV3Client = pipelineV3Client;
        this.repositoryRepository = repositoryRepository;
        this.issueRepository = issueRepository;
        this.gitHubIssueService = gitHubIssueService;
        this.jwtProvider = jwtProvider;
        this.userService = userService;
        this.projectRepository = projectRepository;
    }

    /**
     * 프로젝트 정보와 파이프라인 정보를 결합하여 반환 (API Composition)
     */
    public markoala.fithub.demo.application.dto.response.ProjectPipelineOverviewResponse getProjectPipelineOverview(Long projectId) {
        log.info("[PipelineV3Service] Composing project-pipeline overview for project {}", projectId);
        
        // 1. Spring DB에서 프로젝트 정보 조회
        markoala.fithub.demo.project.Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        // 2. FastAPI에서 파이프라인 정보 조회
        PipelineListResponse pipelineList = pipelineV3Client.getPipelinesByProject(projectId);

        // 3. 데이터 결합
        return new markoala.fithub.demo.application.dto.response.ProjectPipelineOverviewResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                pipelineList.pipelines()
        );
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
     * @return FastAPI 응답 PipelineV3Response (v3 DTO)
     */
    public PipelineV3Response generateV3Pipeline(PipelineV3Request request) {
        log.info("[PipelineV3Service] Generating v3 pipeline — projectId={}, category={}", 
                request.projectId(), request.category());

        PipelineV3Response response = pipelineV3Client.generateV3Pipeline(request);

        log.info("[PipelineV3Service] v3 pipeline generated — id={}, version={}, steps={}",
                response.id(), response.version(),
                response.steps() != null ? response.steps().size() : 0);

        return response;
    }

    // ─────────────────────────────────────────────────────────────────
    // v3 전체 카테고리 파이프라인 일괄 생성
    // ─────────────────────────────────────────────────────────────────

    /**
     * 지정된 카테고리 목록(FE/BE/AI 등)에 대해 v3 파이프라인 일괄 생성.
     *
     * @param projectId  프로젝트 ID
     * @param categories 대상 카테고리 목록
     * @param prdFile    PRD 파일 (선택)
     * @return 카테고리별로 생성된 PipelineV3Response 목록
     */
    public List<PipelineV3Response> generateV3PipelinesForCategories(Long projectId,
                                                                    String requirements,
                                                                    String techStack,
                                                                    List<String> categories,
                                                                    MultipartFile file) {
        if (categories == null || categories.isEmpty()) {
            throw new IllegalArgumentException("대상 카테고리가 지정되지 않았습니다.");
        }

        log.info("[PipelineV3Service] Generating v3 pipelines for project {} — categories: {}", projectId, categories);

        return categories.stream()
                .map(category -> {
                    log.info("[PipelineV3Service] Generating v3 pipeline for category: {}", category);
                    PipelineV3Request request = new PipelineV3Request(projectId, requirements, category, techStack, file);
                    return pipelineV3Client.generateV3Pipeline(request);
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
    public PipelineStepV3Response addStepToPipeline(Long pipelineId, PipelineStepCreateRequest request) {
        log.info("[PipelineV3Service] Adding step to pipeline {}", pipelineId);
        return pipelineV3Client.addPipelineStep(pipelineId, request);
    }

    /**
     * 파이프라인 스텝 수정.
     */
    public PipelineStepV3Response updatePipelineStep(Long stepId, PipelineStepUpdateRequest request) {
        log.info("[PipelineV3Service] Updating pipeline step {}", stepId);
        return pipelineV3Client.updatePipelineStep(stepId, request);
    }

    /**
     * 회의 정보를 기반으로 파이프라인 스텝 최종 승인 처리.
     */
    public PipelineStepV3Response confirmPipelineStep(Long stepId, markoala.fithub.demo.application.dto.request.MeetingStepConfirmationRequest request) {
        log.info("[PipelineV3Service] Confirming pipeline step {} via meeting {}", stepId, request.meetingId());
        return pipelineV3Client.confirmPipelineStep(stepId, request);
    }

    /**
     * 파이프라인 단건 조회.
     */
    public PipelineV3Response getPipeline(Long pipelineId) {
        log.info("[PipelineV3Service] Fetching pipeline {}", pipelineId);
        return pipelineV3Client.getPipeline(pipelineId);
    }

    /**
     * 파이프라인 삭제.
     */
    public void deletePipeline(Long pipelineId) {
        log.info("[PipelineV3Service] Deleting pipeline {}", pipelineId);
        pipelineV3Client.deletePipeline(pipelineId);
    }

    // ─────────────────────────────────────────────────────────────────
    // 파이프라인 스텝 → Issue 변환 및 동기화
    // ─────────────────────────────────────────────────────────────────

    /**
     * v3 파이프라인 스텝을 Issue로 변환 + GitHub 동기화
     */
    public markoala.fithub.demo.issue.Issue createIssueFromPipelineStepAndSync(Long pipelineStepId, Long repositoryId, String title, String description, String repoUrl, String authHeader) {
        log.info("[PipelineV3Service] Creating issue and syncing to GitHub: {}", title);

        // 1. Issue 생성 (DB)
        markoala.fithub.demo.issue.Issue issue = markoala.fithub.demo.issue.Issue.createIssue(repositoryId, null, title, description, "PENDING");
        issue.setPipelineStepId(pipelineStepId.intValue());
        markoala.fithub.demo.issue.Issue savedIssue = issueRepository.save(issue);

        // 2. GitHub에 동기화
        try {
            String token = authHeader.substring(7); // "Bearer " 제거
            Long userId = jwtProvider.getUserIdFromToken(token);
            markoala.fithub.demo.user.User user = userService.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
            String githubAccessToken = user.getGithubAccessToken();

            gitHubIssueService.syncIssueToGitHub(savedIssue, repoUrl, githubAccessToken);
            log.info("[PipelineV3Service] Issue {} synced to GitHub", savedIssue.getId());
        } catch (Exception e) {
            log.error("[PipelineV3Service] Failed to sync issue to GitHub: {}", e.getMessage());
        }

        return savedIssue;
    }
 
    /**
     * 파이프라인 스텝 삭제.
     */
    public void deletePipelineStep(Long stepId) {
        log.info("[PipelineV3Service] Deleting pipeline step {}", stepId);
        pipelineV3Client.deletePipelineStep(stepId);
    }

    /**
     * Ouroboros 사전 인터뷰 프록시
     */
    public Object ouroborosInterview(Object requestBody) {
        log.info("[PipelineV3Service] Proxying interview request to FastAPI");
        return pipelineV3Client.ouroborosInterview(requestBody);
    }
}
