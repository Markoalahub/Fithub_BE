package markoala.fithub.demo.pipeline;

import markoala.fithub.demo.issue.GithubRepository;
import markoala.fithub.demo.issue.Issue;
import markoala.fithub.demo.issue.IssueRepository;
import markoala.fithub.demo.issue.IssueSync;
import markoala.fithub.demo.issue.IssueSyncRepository;
import markoala.fithub.demo.issue.RepositoryRepository;
import markoala.fithub.demo.issue.GitHubIssueService;
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
     * FastAPI에 파이프라인 생성 요청하고, 반환된 스텝들을 Spring DB에 Issue로 저장
     * @param projectId Spring Project ID
     * @param requirements 요구사항 텍스트
     * @param category 카테고리
     * @return PipelineResponse (FastAPI에서 생성된 파이프라인)
     */
    public PipelineResponse generatePipeline(Long projectId, String requirements, String category) {
        log.info("[Pipeline Service] Generating pipeline for project {} with category: {}", projectId, category);

        // FastAPI에 파이프라인 생성 요청
        PipelineResponse pipelineResponse = pipelineClient.generateAndSavePipeline(projectId, requirements, category);

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
     * 프로젝트의 파이프라인 목록 조회
     */
    public PipelineListResponse getPipelinesByProject(Long projectId) {
        log.info("[Pipeline Service] Fetching pipelines for project {}", projectId);
        return pipelineClient.getPipelinesByProject(projectId);
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
