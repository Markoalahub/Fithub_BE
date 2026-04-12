package markoala.fithub.demo.integration;

import markoala.fithub.demo.github.dto.GithubRepositoryDto;
import markoala.fithub.demo.github.service.GithubRepositoryService;
import markoala.fithub.demo.global.security.jwt.JwtProvider;
import markoala.fithub.demo.issue.GithubRepository;
import markoala.fithub.demo.issue.GitHubIssueService;
import markoala.fithub.demo.issue.Issue;
import markoala.fithub.demo.issue.IssueRepository;
import markoala.fithub.demo.issue.IssueSync;
import markoala.fithub.demo.issue.IssueSyncRepository;
import markoala.fithub.demo.issue.RepositoryRepository;
import markoala.fithub.demo.pipeline.PipelineClient;
import markoala.fithub.demo.pipeline.dto.PipelineResponse;
import markoala.fithub.demo.pipeline.dto.PipelineStepResponse;
import markoala.fithub.demo.project.Project;
import markoala.fithub.demo.project.ProjectRepository;
import markoala.fithub.demo.user.User;
import markoala.fithub.demo.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("End-to-End Pipeline Integration Test")
class EndToEndPipelineIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private RepositoryRepository repositoryRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private IssueSyncRepository issueSyncRepository;

    @MockBean
    private GithubRepositoryService githubRepositoryService;

    @MockBean
    private PipelineClient pipelineClient;

    @MockBean
    private GitHubIssueService gitHubIssueService;

    @MockBean
    private JwtProvider jwtProvider;

    @MockBean
    private UserService userService;

    private Project testProject;
    private GithubRepository testRepository;
    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_JWT_TOKEN = "test-jwt-token-12345";

    @BeforeEach
    void setUp() {
        // Clean up
        issueSyncRepository.deleteAll();
        issueRepository.deleteAll();
        repositoryRepository.deleteAll();
        projectRepository.deleteAll();

        // Create test project
        testProject = Project.createProject("Test Project", "Test Description");
        testProject = projectRepository.save(testProject);

        // Setup JWT mock
        User testUser = new User(TEST_USER_ID, "testuser", "test@example.com", "USER", "github123", "ghp_test_token", null, null);
        when(jwtProvider.validateToken(TEST_JWT_TOKEN)).thenReturn(true);
        when(jwtProvider.getUserIdFromToken(TEST_JWT_TOKEN)).thenReturn(TEST_USER_ID);
        when(userService.findById(TEST_USER_ID)).thenReturn(java.util.Optional.of(testUser));
    }

    @Test
    @DisplayName("Step 1: GitHub repo 동기화")
    void step1_SyncGithubRepository() throws Exception {
        // Mock GitHub API response
        var mockGithubRepos = List.of(
            new GithubRepositoryDto(
                1207474638L,
                "travel-plan",
                "KYH-99/travel-plan",
                "https://github.com/KYH-99/travel-plan",
                "Travel planning app",
                false,
                1,
                1,
                "Python",
                "2024-01-01T00:00:00Z",
                "2024-04-12T00:00:00Z"
            )
        );
        when(githubRepositoryService.getMyRepos()).thenReturn(mockGithubRepos);

        // Sync repository
        String requestBody = """
            {
              "githubRepoIds": [1207474638],
              "categoryMappings": [
                {
                  "githubRepoId": 1207474638,
                  "repoName": "travel-plan",
                  "category": "BE"
                }
              ]
            }
            """;

        mockMvc.perform(post("/api/v1/projects/{projectId}/repositories/sync-from-github", testProject.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].category").value("BE"))
                .andExpect(jsonPath("$[0].repoUrl").value("https://github.com/KYH-99/travel-plan"));

        // Verify repository saved in DB
        testRepository = repositoryRepository.findByProjectId(testProject.getId()).get(0);
        assert testRepository != null;
        assert testRepository.getCategory().equals("BE");
    }

    @Test
    @DisplayName("Step 2: 파이프라인 생성")
    void step2_GeneratePipeline() throws Exception {
        // Setup: Create repository first
        testRepository = GithubRepository.createRepository(
            testProject.getId(),
            "https://github.com/KYH-99/travel-plan",
            "GITHUB",
            "BE"
        );
        testRepository = repositoryRepository.save(testRepository);

        // Mock FastAPI response
        var mockPipeline = new PipelineResponse(
            1L,
            testProject.getId(),
            "BE",
            1L,
            true,
            List.of(
                new PipelineStepResponse(1L, 1L, "API 설계", "REST API 설계", false, "ai_generated"),
                new PipelineStepResponse(2L, 1L, "데이터베이스 설계", "DB 스키마 설계", false, "ai_generated"),
                new PipelineStepResponse(3L, 1L, "인증 구현", "JWT 기반 인증", false, "ai_generated")
            )
        );

        when(pipelineClient.generateAndSavePipeline(
            eq(testProject.getId()),
            eq("BE"),
            isNull(),
            any()
        )).thenReturn(mockPipeline);

        // Generate pipeline
        mockMvc.perform(post("/api/v1/pipelines/generate-all")
                .param("projectId", testProject.getId().toString())
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pipelines[0].steps.length()").value(3))
                .andExpect(jsonPath("$.pipelines[0].steps[0].title").value("API 설계"))
                .andExpect(jsonPath("$.pipelines[0].steps[1].title").value("데이터베이스 설계"))
                .andExpect(jsonPath("$.pipelines[0].steps[2].title").value("인증 구현"));
    }

    @Test
    @DisplayName("Step 3: Pipeline Step 수정")
    void step3_UpdatePipelineStep() throws Exception {
        // Mock FastAPI response
        var updatedStep = new PipelineStepResponse(
            1L,
            1L,
            "수정된 API 설계",
            "REST API 및 GraphQL 설계",
            false,
            "user_created"
        );

        when(pipelineClient.updatePipelineStep(
            eq(1L),
            argThat(req -> req.title().equals("수정된 API 설계"))
        )).thenReturn(updatedStep);

        // Update step
        String requestBody = """
            {
              "title": "수정된 API 설계",
              "description": "REST API 및 GraphQL 설계",
              "is_completed": false
            }
            """;

        mockMvc.perform(put("/api/v1/pipelines/steps/{stepId}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("수정된 API 설계"))
                .andExpect(jsonPath("$.description").value("REST API 및 GraphQL 설계"));
    }

    @Test
    @DisplayName("Step 4: Issue 생성 (Pipeline Step → Issue)")
    void step4_CreateIssueFromPipelineStep() throws Exception {
        // Setup: Create repository
        testRepository = GithubRepository.createRepository(
            testProject.getId(),
            "https://github.com/KYH-99/travel-plan",
            "GITHUB",
            "BE"
        );
        testRepository = repositoryRepository.save(testRepository);

        // Create issue
        String requestBody = """
            {
              "repositoryId": %d,
              "title": "API 설계",
              "description": "REST API 설계 및 문서화"
            }
            """.formatted(testRepository.getId());

        mockMvc.perform(post("/api/v1/pipelines/steps/{pipelineStepId}/create-issue", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("API 설계"))
                .andExpect(jsonPath("$.description").value("REST API 설계 및 문서화"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        // Verify issue saved in DB
        List<Issue> issues = issueRepository.findByRepositoryId(testRepository.getId());
        assert issues.size() == 1;
        assert issues.get(0).getTitle().equals("API 설계");
        assert issues.get(0).getPipelineStepId() == 1;
    }

    @Test
    @DisplayName("Step 5: Issue를 GitHub로 동기화")
    void step5_SyncIssueToGitHub() throws Exception {
        // Setup: Create repository and issue
        testRepository = GithubRepository.createRepository(
            testProject.getId(),
            "https://github.com/KYH-99/travel-plan",
            "GITHUB",
            "BE"
        );
        testRepository = repositoryRepository.save(testRepository);

        Issue testIssue = Issue.createIssue(
            testRepository.getId(),
            null,
            "API 설계",
            "REST API 설계 및 문서화",
            "PENDING"
        );
        testIssue.setPipelineStepId(1);
        testIssue = issueRepository.save(testIssue);

        // Mock GitHub sync response
        IssueSync mockSync = IssueSync.createSync(
            testIssue.getId(),
            42,  // GitHub issue number
            testRepository.getId(),
            "SYNCED"
        );
        mockSync.markSynced("https://github.com/KYH-99/travel-plan/issues/42");

        when(gitHubIssueService.syncIssueToGitHub(
            any(),
            eq("https://github.com/KYH-99/travel-plan"),
            anyString()
        )).thenReturn(mockSync);

        // Sync to GitHub
        mockMvc.perform(post("/api/v1/issues/{issueId}/sync", testIssue.getId())
                .param("repoUrl", "https://github.com/KYH-99/travel-plan")
                .header("Authorization", "Bearer " + TEST_JWT_TOKEN))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SYNCED"))
                .andExpect(jsonPath("$.githubIssueNumber").value(42))
                .andExpect(jsonPath("$.githubUrl").value("https://github.com/KYH-99/travel-plan/issues/42"));
    }

    @Test
    @DisplayName("Full E2E: 전체 시나리오 통합 테스트")
    void endToEnd_FullScenario() throws Exception {
        // 1. Sync GitHub Repository
        var mockGithubRepos = List.of(
            new GithubRepositoryDto(
                1207474638L, "travel-plan", "KYH-99/travel-plan",
                "https://github.com/KYH-99/travel-plan", "Travel planning app",
                false, 1, 1, "Python", "2024-01-01T00:00:00Z", "2024-04-12T00:00:00Z"
            )
        );
        when(githubRepositoryService.getMyRepos()).thenReturn(mockGithubRepos);

        String syncBody = """
            {
              "githubRepoIds": [1207474638],
              "categoryMappings": [
                {
                  "githubRepoId": 1207474638,
                  "repoName": "travel-plan",
                  "category": "BE"
                }
              ]
            }
            """;

        var syncResponse = mockMvc.perform(
                post("/api/v1/projects/{projectId}/repositories/sync-from-github", testProject.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(syncBody))
                .andExpect(status().isCreated())
                .andReturn();

        testRepository = repositoryRepository.findByProjectId(testProject.getId()).get(0);

        // 2. Generate Pipeline
        var mockPipeline = new PipelineResponse(
            1L, testProject.getId(), "BE", 1L, true,
            List.of(
                new PipelineStepResponse(1L, 1L, "API 설계", "REST API", false, "ai_generated"),
                new PipelineStepResponse(2L, 1L, "DB 설계", "Schema", false, "ai_generated")
            )
        );
        when(pipelineClient.generateAndSavePipeline(
            eq(testProject.getId()), eq("BE"), isNull(), any()
        )).thenReturn(mockPipeline);

        mockMvc.perform(post("/api/v1/pipelines/generate-all")
                .param("projectId", testProject.getId().toString())
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pipelines[0].steps.length()").value(2));

        // 3. Update Pipeline Step
        var updatedStep = new PipelineStepResponse(
            1L, 1L, "수정된 API 설계", "GraphQL + REST", false, "user_created"
        );
        when(pipelineClient.updatePipelineStep(eq(1L), any())).thenReturn(updatedStep);

        mockMvc.perform(put("/api/v1/pipelines/steps/{stepId}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"수정된 API 설계\",\"description\":\"GraphQL + REST\",\"is_completed\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("수정된 API 설계"));

        // 4. Create Issue
        String issueBody = """
            {
              "repositoryId": %d,
              "title": "수정된 API 설계",
              "description": "GraphQL + REST 설계"
            }
            """.formatted(testRepository.getId());

        mockMvc.perform(post("/api/v1/pipelines/steps/{pipelineStepId}/create-issue", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(issueBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("수정된 API 설계"));

        Issue createdIssue = issueRepository.findByRepositoryId(testRepository.getId()).get(0);

        // 5. Sync to GitHub
        IssueSync mockSync = IssueSync.createSync(createdIssue.getId(), 100, testRepository.getId(), "SYNCED");
        mockSync.markSynced("https://github.com/KYH-99/travel-plan/issues/100");

        when(gitHubIssueService.syncIssueToGitHub(
            any(), eq("https://github.com/KYH-99/travel-plan"), anyString()
        )).thenReturn(mockSync);

        mockMvc.perform(post("/api/v1/issues/{issueId}/sync", createdIssue.getId())
                .param("repoUrl", "https://github.com/KYH-99/travel-plan")
                .header("Authorization", "Bearer " + TEST_JWT_TOKEN))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SYNCED"))
                .andExpect(jsonPath("$.githubIssueNumber").value(100));
    }
}
