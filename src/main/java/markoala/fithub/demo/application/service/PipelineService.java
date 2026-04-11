package markoala.fithub.demo.application.service;

import markoala.fithub.demo.application.client.PipelineClient;
import markoala.fithub.demo.application.dto.request.PipelineStepCreateRequest;
import markoala.fithub.demo.application.dto.request.PipelineStepUpdateRequest;
import markoala.fithub.demo.application.dto.response.MultiPipelineResponse;
import markoala.fithub.demo.application.dto.response.PipelineListResponse;
import markoala.fithub.demo.application.dto.response.PipelineResponse;
import markoala.fithub.demo.application.dto.response.PipelineStepResponse;
import markoala.fithub.demo.domain.issue.entity.GithubRepository;
import markoala.fithub.demo.domain.issue.entity.Issue;
import markoala.fithub.demo.domain.issue.entity.IssueSync;
import markoala.fithub.demo.domain.issue.repository.IssueRepository;
import markoala.fithub.demo.domain.issue.repository.IssueSyncRepository;
import markoala.fithub.demo.domain.issue.repository.RepositoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class PipelineService {

    private static final Logger log = LoggerFactory.getLogger(PipelineService.class);

    private final PipelineClient pipelineClient;
    private final GitHubIssueService gitHubIssueService;
    private final IssueRepository issueRepository;
    private final IssueSyncRepository issueSyncRepository;
    private final RepositoryRepository repositoryRepository;

    public PipelineService(
            PipelineClient pipelineClient,
            GitHubIssueService gitHubIssueService,
            IssueRepository issueRepository,
            IssueSyncRepository issueSyncRepository,
            RepositoryRepository repositoryRepository
    ) {
        this.pipelineClient = pipelineClient;
        this.gitHubIssueService = gitHubIssueService;
        this.issueRepository = issueRepository;
        this.issueSyncRepository = issueSyncRepository;
        this.repositoryRepository = repositoryRepository;
    }

    /**
     * FastAPI에 파이프라인 생성 요청하고, 반환된 스텝들을 Spring DB에 Issue로 저장
     * @param projectId Spring Project ID
     * @param requirements 요구사항 텍스트
     * @param category 카테고리
     * @return PipelineResponse (FastAPI에서 생성된 파이프라인)
     */
    public PipelineResponse generatePipeline(Long projectId, String requirements, String category) {
        log.info("[Pipeline Service] Generating pipeline for project {} with category: {}", projectId, category);

        // FastAPI에 파이프라인 생성 요청
        PipelineResponse pipelineResponse = pipelineClient.generateAndSavePipeline(projectId, requirements, category, null);

        log.info("[Pipeline Service] Pipeline generated with {} steps", pipelineResponse.steps().size());

        // 파이프라인 스텝들을 Spring Issue로 저장 (추후 GitHub 동기화용)
        for (PipelineStepResponse step : pipelineResponse.steps()) {
            Issue issue = Issue.createIssue(
                    null, // repositoryId는 나중에 설정
                    null, // githubIssueNumber는 GitHub 동기화 후 설정
                    step.title(),
                    step.description(),
                    "PENDING"
            );
            issue.setPipelineStepId(step.id().intValue());
            issueRepository.save(issue);
            log.info("[Pipeline Service] Issue created from pipeline step {}: {}", step.id(), step.title());
        }

        return pipelineResponse;
    }

    /**
     * 프로젝트의 모든 직군(category) 별로 파이프라인을 일괄 생성 (API Composition)
     * - 프로젝트에 등록된 레포 목록에서 category를 추출
     * - 각 category 별로 FastAPI 파이프라인 생성 호출
     * - 생성된 스텝들을 각 레포의 Spring Issue로 저장
     */
    public MultiPipelineResponse generatePipelinesForAllCategories(Long projectId, String requirements, byte[] pdfBytes) {
        log.info("[Pipeline Service] Generating pipelines for all categories in project {}", projectId);

        List<GithubRepository> repositories = repositoryRepository.findByProjectId(projectId);
        if (repositories.isEmpty()) {
            throw new IllegalArgumentException("No repositories found for project: " + projectId);
        }

        List<PipelineResponse> createdPipelines = new ArrayList<>();

        for (GithubRepository repo : repositories) {
            String category = repo.getCategory();
            log.info("[Pipeline Service] Generating pipeline for category '{}' (repoId={})", category, repo.getId());

            PipelineResponse pipelineResponse = pipelineClient.generateAndSavePipeline(projectId, requirements, category, pdfBytes);

            for (PipelineStepResponse step : pipelineResponse.steps()) {
                Issue issue = Issue.createIssue(repo.getId(), null, step.title(), step.description(), "PENDING");
                issue.setPipelineStepId(step.id().intValue());
                issueRepository.save(issue);
                log.info("[Pipeline Service] Issue saved for step {}: {} (repo={})",
                        step.id(), step.title(), repo.getId());
            }

            createdPipelines.add(pipelineResponse);
        }

        log.info("[Pipeline Service] {} pipelines created for project {}", createdPipelines.size(), projectId);
        return new MultiPipelineResponse(projectId, createdPipelines.size(), createdPipelines);
    }

    /**
     * 프로젝트의 파이프라인 목록 조회 (전체)
     */
    public PipelineListResponse getPipelinesByProject(Long projectId) {
        log.info("[Pipeline Service] Fetching all pipelines for project {}", projectId);
        return pipelineClient.getPipelinesByProject(projectId);
    }

    /**
     * 프로젝트의 파이프라인 목록 조회 (category 필터)
     */
    public PipelineListResponse getPipelinesByProject(Long projectId, String category) {
        log.info("[Pipeline Service] Fetching pipelines for project {} with category '{}'", projectId, category);
        return pipelineClient.getPipelinesByProject(projectId, category);
    }

    /**
     * 파이프라인에 새로운 스텝 추가
     */
    public PipelineStepResponse addStepToPipeline(Long pipelineId, String title, String description) {
        log.info("[Pipeline Service] Adding step to pipeline {}: {}", pipelineId, title);

        PipelineStepCreateRequest request = new PipelineStepCreateRequest(
                title,
                description,
                false,
                "user_created"
        );

        PipelineStepResponse stepResponse = pipelineClient.addPipelineStep(pipelineId, request);

        // Spring DB에도 Issue로 저장
        Issue issue = Issue.createIssue(
                null,
                null,
                stepResponse.title(),
                stepResponse.description(),
                "PENDING"
        );
        issue.setPipelineStepId(stepResponse.id().intValue());
        issueRepository.save(issue);

        log.info("[Pipeline Service] Step {} added to pipeline {}", stepResponse.id(), pipelineId);
        return stepResponse;
    }

    /**
     * 파이프라인 스텝을 완료 처리
     */
    public void completeStep(Long stepId) {
        log.info("[Pipeline Service] Completing pipeline step {}", stepId);
        pipelineClient.updatePipelineStep(stepId, new PipelineStepUpdateRequest(true));
    }

    /**
     * 오케스트레이션: 파이프라인 스텝들을 GitHub Issues로 동기화
     * @param pipelineId FastAPI Pipeline ID
     * @param repositoryId Spring Repository ID
     * @param accessToken GitHub Access Token
     */
    public List<IssueSync> syncPipelineToGitHub(Long pipelineId, Long repositoryId, String accessToken) {
        log.info("[Pipeline Service] Syncing pipeline {} to GitHub repository {}", pipelineId, repositoryId);

        // Repository 정보 조회
        GithubRepository repository = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new IllegalArgumentException("Repository not found: " + repositoryId));

        // 파이프라인의 모든 Issue 조회
        List<Issue> issues = issueRepository.findByRepositoryId(repositoryId);

        // 각 Issue를 GitHub로 동기화
        return issues.stream()
                .map(issue -> {
                    IssueSync sync = gitHubIssueService.syncIssueToGitHub(issue, repository.getRepoUrl(), accessToken);
                    log.info("[Pipeline Sync] Issue {} synced: {}", issue.getId(), sync.getStatus());
                    return sync;
                })
                .collect(Collectors.toList());
    }
}
