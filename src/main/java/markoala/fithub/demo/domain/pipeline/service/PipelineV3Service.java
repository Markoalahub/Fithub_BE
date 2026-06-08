package markoala.fithub.demo.domain.pipeline.service;

import markoala.fithub.demo.domain.issue.GitHubIssueService;
import markoala.fithub.demo.domain.issue.Issue;
import markoala.fithub.demo.domain.issue.IssueRepository;
import markoala.fithub.demo.domain.pipeline.client.PipelineV3Client;

import markoala.fithub.demo.domain.meeting.dto.request.MeetingStepConfirmationRequest;
import markoala.fithub.demo.domain.pipeline.dto.request.PipelineGithubRepositoryUpdateRequest;
import markoala.fithub.demo.domain.pipeline.dto.request.PipelineStepCreateRequest;
import markoala.fithub.demo.domain.pipeline.dto.request.PipelineStepUpdateRequest;
import markoala.fithub.demo.domain.pipeline.dto.request.PipelineV3Request;
import markoala.fithub.demo.domain.pipeline.dto.response.PipelineListResponse;
import markoala.fithub.demo.domain.pipeline.dto.response.PipelineV3Response;
import markoala.fithub.demo.domain.pipeline.dto.response.PipelineStepV3Response;
import markoala.fithub.demo.domain.issue.RepositoryRepository;
import markoala.fithub.demo.domain.pipeline.dto.response.ProjectPipelineOverviewResponse;
import markoala.fithub.demo.domain.pipeline.dto.response.ProjectPipelineSummaryListResponse;
import markoala.fithub.demo.domain.project.Project;
import markoala.fithub.demo.domain.project.ProjectMemberRepository;
import markoala.fithub.demo.domain.project.ProjectRepository;
import markoala.fithub.demo.domain.user.User;
import markoala.fithub.demo.domain.user.UserService;
import markoala.fithub.demo.global.security.jwt.JwtProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

/**
 * v3 파이프라인 생성 서비스 계층.
 *
 * <p>{@link PipelineV3Client}를 통해 FastAPI의 {@code /pipelines/generate-v3}
 * 엔드포인트를 호출하고, 응답 DTO를 그대로 반환하거나 필요한 비즈니스 로직을 수행합니다.</p>
 */
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PipelineV3Service {

    private static final Logger log = LoggerFactory.getLogger(PipelineV3Service.class);

    private final PipelineV3Client pipelineV3Client;
    private final RepositoryRepository repositoryRepository;
    private final IssueRepository issueRepository;
    private final GitHubIssueService gitHubIssueService;
    private final JwtProvider jwtProvider;
    private final UserService userService;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private static final List<String> ALL_CATEGORIES = List.of("BE", "FE");

    /**
     * 프로젝트 정보와 파이프라인 정보를 결합하여 반환 (API Composition)
     */
    public ProjectPipelineOverviewResponse getProjectPipelineOverview(Long projectId) {
        log.info("[PipelineV3Service] Composing project-pipeline overview for project {}", projectId);
        
        // 1. Spring DB에서 프로젝트 정보 조회
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다: " + projectId));

        // 2. FastAPI에서 파이프라인 정보 조회
        PipelineListResponse pipelineList = pipelineV3Client.getPipelinesByProject(projectId);

        // 3. 데이터 결합
        return new ProjectPipelineOverviewResponse(
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
     * @return category가 ALL이면 PipelineListResponse, 그 외에는 PipelineV3Response
     */
    public Object generateV3Pipeline(Long userId, PipelineV3Request request) {
        log.info("[PipelineV3Service] Generating v3 pipeline — userId={}, projectId={}, category={}",
                userId, request.projectId(), request.category());

        projectRepository.findById(request.projectId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 프로젝트 ID 입니다."
                ));

        validateProjectMember(userId, request.projectId());

        String category = request.category() == null ? "BE" : request.category().trim().toUpperCase();
        int quotaAmount = "ALL".equals(category) ? ALL_CATEGORIES.size() : 1;
        userService.consumeAiPipelineGenerationQuota(userId, quotaAmount);
        try {
            if ("ALL".equals(category)) {
                List<PipelineV3Response> pipelines = ALL_CATEGORIES.stream()
                        .map(categoryName -> pipelineV3Client.generateV3Pipeline(new PipelineV3Request(
                                request.projectId(),
                                request.requirements(),
                                categoryName,
                                request.techStack(),
                                request.file()
                        )))
                        .toList();

                log.info("[PipelineV3Service] v3 ALL pipelines generated — userId={}, projectId={}, total={}",
                        userId, request.projectId(), pipelines.size());

                return new PipelineListResponse(pipelines, (long) pipelines.size());
            }

            PipelineV3Response response = pipelineV3Client.generateV3Pipeline(request);

            log.info("[PipelineV3Service] v3 pipeline generated — userId={}, id={}, version={}, feats={}",
                    userId, response.id(), response.version(),
                    response.feats() != null ? response.feats().size() : 0);

            return response;
        } catch (RuntimeException e) {
            userService.restoreAiPipelineGenerationQuota(userId, quotaAmount);
            throw e;
        }
    }

    private void validateProjectMember(Long userId, Long projectId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }

        projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "해당 프로젝트에 속한 사용자만 파이프라인을 생성할 수 있습니다."
                ));
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
     * 프로젝트별 파이프라인 요약 목록 조회.
     */
    public ProjectPipelineSummaryListResponse getProjectPipelineSummaries(Long projectId) {
        log.info("[PipelineV3Service] Fetching pipeline summaries for project {}", projectId);
        return pipelineV3Client.getProjectPipelineSummaries(projectId);
    }

    /**
     * 프로젝트 + 카테고리 기준 최신 파이프라인 조회.
     */
    public PipelineV3Response getLatestProjectPipeline(Long projectId, String category) {
        log.info("[PipelineV3Service] Fetching latest pipeline for project {}, category {}", projectId, category);
        return pipelineV3Client.getLatestProjectPipeline(projectId, category);
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
    public PipelineStepV3Response confirmPipelineStep(Long stepId, MeetingStepConfirmationRequest request) {
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
     * 파이프라인 GitHub repository URL 연결.
     */
    public PipelineV3Response updatePipelineGithubRepository(Long pipelineId, PipelineGithubRepositoryUpdateRequest request) {
        log.info("[PipelineV3Service] Updating pipeline {} GitHub repository URL", pipelineId);
        return pipelineV3Client.updatePipelineGithubRepository(pipelineId, request);
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
    public Issue createIssueFromPipelineStepAndSync(Long pipelineStepId, Long repositoryId, String title, String description, String repoUrl, String authHeader) {
        log.info("[PipelineV3Service] Creating issue and syncing to GitHub: {}", title);

        // 1. Issue 생성 (DB)
        Issue issue = Issue.createIssue(repositoryId, null, title, description, "PENDING");
        issue.setPipelineStepId(pipelineStepId.intValue());
        Issue savedIssue = issueRepository.save(issue);

        // 2. GitHub에 동기화
        try {
            String token = authHeader.substring(7); // "Bearer " 제거
            Long userId = jwtProvider.getUserIdFromToken(token);
            User user = userService.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
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
}
