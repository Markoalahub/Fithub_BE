package markoala.fithub.demo.pipeline;

import markoala.fithub.demo.issue.GithubRepository;
import markoala.fithub.demo.issue.Issue;
import markoala.fithub.demo.issue.IssueRepository;
import markoala.fithub.demo.issue.IssueSync;
import markoala.fithub.demo.issue.IssueSyncRepository;
import markoala.fithub.demo.issue.RepositoryRepository;
import markoala.fithub.demo.issue.GitHubIssueService;
import markoala.fithub.demo.pipeline.dto.MultiPipelineResponse;
import markoala.fithub.demo.pipeline.dto.PipelineListResponse;
import markoala.fithub.demo.pipeline.dto.PipelineResponse;
import markoala.fithub.demo.pipeline.dto.PipelineStepCreateRequest;
import markoala.fithub.demo.pipeline.dto.PipelineStepResponse;
import markoala.fithub.demo.pipeline.dto.PipelineStepUpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     * 프로젝트 내 모든 카테고리에 대해 파이프라인 생성 (PDF PRD 지원)
     * 프로젝트의 GithubRepository 목록에서 category를 추출해 카테고리별로 FastAPI 호출
     */
    public MultiPipelineResponse generatePipelinesForAllCategories(Long projectId, byte[] pdfBytes) {
        List<GithubRepository> repos = repositoryRepository.findByProjectId(projectId);

        List<String> categories = repos.stream()
                .map(GithubRepository::getCategory)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .collect(Collectors.toList());

        if (categories.isEmpty()) {
            throw new IllegalArgumentException("No categorized repositories found for project: " + projectId);
        }

        log.info("[Pipeline Service] Generating pipelines for project {} categories: {}", projectId, categories);

        List<PipelineResponse> pipelines = categories.stream()
                .map(category -> {
                    log.info("[Pipeline Service] Generating pipeline for category: {}", category);
                    PipelineResponse response = pipelineClient.generateAndSavePipeline(projectId, category, null, pdfBytes);
                    savePipelineStepsAsIssues(response);
                    return response;
                })
                .collect(Collectors.toList());

        return new MultiPipelineResponse(projectId, pipelines.size(), pipelines);
    }

    /**
     * 단일 파이프라인 생성
     */
    public PipelineResponse generatePipeline(Long projectId, String requirements, String category) {
        log.info("[Pipeline Service] Generating pipeline for project {} with category: {}", projectId, category);
        PipelineResponse pipelineResponse = pipelineClient.generateAndSavePipeline(projectId, category, requirements, null);
        log.info("[Pipeline Service] Pipeline generated with {} steps", pipelineResponse.steps().size());
        savePipelineStepsAsIssues(pipelineResponse);
        return pipelineResponse;
    }

    private void savePipelineStepsAsIssues(PipelineResponse pipelineResponse) {
        for (PipelineStepResponse step : pipelineResponse.steps()) {
            Issue issue = Issue.createIssue(null, null, step.title(), step.description(), "PENDING");
            issue.setPipelineStepId(step.id().intValue());
            issueRepository.save(issue);
        }
    }

    public PipelineListResponse getPipelinesByProject(Long projectId) {
        log.info("[Pipeline Service] Fetching pipelines for project {}", projectId);
        return pipelineClient.getPipelinesByProject(projectId);
    }

    public PipelineStepResponse addStepToPipeline(Long pipelineId, String title, String description) {
        log.info("[Pipeline Service] Adding step to pipeline {}: {}", pipelineId, title);
        PipelineStepCreateRequest request = new PipelineStepCreateRequest(title, description, false, "user_created");
        PipelineStepResponse stepResponse = pipelineClient.addPipelineStep(pipelineId, request);

        Issue issue = Issue.createIssue(null, null, stepResponse.title(), stepResponse.description(), "PENDING");
        issue.setPipelineStepId(stepResponse.id().intValue());
        issueRepository.save(issue);

        return stepResponse;
    }

    public void completeStep(Long stepId) {
        log.info("[Pipeline Service] Completing pipeline step {}", stepId);
        pipelineClient.updatePipelineStep(stepId, new PipelineStepUpdateRequest(true));
    }

    public List<IssueSync> syncPipelineToGitHub(Long pipelineId, Long repositoryId, String accessToken) {
        log.info("[Pipeline Service] Syncing pipeline {} to GitHub repository {}", pipelineId, repositoryId);
        GithubRepository repository = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new IllegalArgumentException("Repository not found: " + repositoryId));

        List<Issue> issues = issueRepository.findByRepositoryId(repositoryId);
        return issues.stream()
                .map(issue -> {
                    IssueSync sync = gitHubIssueService.syncIssueToGitHub(issue, repository.getRepoUrl(), accessToken);
                    log.info("[Pipeline Sync] Issue {} synced: {}", issue.getId(), sync.getStatus());
                    return sync;
                })
                .collect(Collectors.toList());
    }
}
