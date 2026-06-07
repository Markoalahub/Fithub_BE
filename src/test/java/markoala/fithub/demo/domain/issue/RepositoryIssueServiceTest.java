package markoala.fithub.demo.domain.issue;

import markoala.fithub.demo.domain.auth.GithubWebClientService;
import markoala.fithub.demo.domain.issue.dto.RepositoryIssueCreateRequest;
import markoala.fithub.demo.domain.issue.dto.RepositoryIssueCreateResponse;
import markoala.fithub.demo.domain.pipeline.client.PipelineV3Client;
import markoala.fithub.demo.domain.pipeline.dto.response.PipelineV3Response;
import markoala.fithub.demo.domain.user.User;
import markoala.fithub.demo.domain.user.UserService;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RepositoryIssueServiceTest {

    @Test
    @DisplayName("파이프라인에 연결된 GitHub 레포지토리에 feat_detail을 제목으로 이슈를 생성한다")
    void createIssue_CreatesGithubIssueOnPipelineRepository() throws Exception {
        UserService userService = mock(UserService.class);
        PipelineV3Client pipelineV3Client = mock(PipelineV3Client.class);
        User user = new User(1L, "developer", "dev", "dev@test.com", "social", true, "gho_test_token", null, null, null);

        when(userService.findById(1L)).thenReturn(Optional.of(user));
        when(pipelineV3Client.getPipeline(33L)).thenReturn(new PipelineV3Response(
                33L,
                1L,
                "BE",
                1,
                "Spring Boot",
                "https://github.com/Markoalahub/Fithub_BE",
                List.of()
        ));

        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("""
                            {
                              "id": 10,
                              "name": "Fithub_BE",
                              "full_name": "Markoalahub/Fithub_BE",
                              "html_url": "https://github.com/Markoalahub/Fithub_BE",
                              "description": "Spring server",
                              "private": false,
                              "stargazers_count": 3,
                              "open_issues_count": 2,
                              "language": "Java",
                              "created_at": "2026-01-01T00:00:00Z",
                              "updated_at": "2026-06-05T00:00:00Z",
                              "default_branch": "main",
                              "pushed_at": "2026-06-05T01:00:00Z",
                              "clone_url": "https://github.com/Markoalahub/Fithub_BE.git"
                            }
                            """));
            server.enqueue(new MockResponse()
                    .setResponseCode(201)
                    .setHeader("Content-Type", "application/json")
                    .setBody("""
                            {
                              "number": 42,
                              "title": "로그인 API 구현",
                              "body": "JWT 기반 로그인 API를 구현합니다.",
                              "state": "open",
                              "html_url": "https://github.com/Markoalahub/Fithub_BE/issues/42"
                            }
                            """));
            server.start();

            GithubWebClientService webClientService = new GithubWebClientService(userService);
            ReflectionTestUtils.setField(webClientService, "githubApiBaseUrl", server.url("/").toString());
            RepositoryIssueService repositoryIssueService = new RepositoryIssueService(
                    userService,
                    webClientService,
                    pipelineV3Client
            );

            RepositoryIssueCreateResponse response = repositoryIssueService.createIssue(
                    1L,
                    33L,
                    new RepositoryIssueCreateRequest("  로그인 API 구현  ", "JWT 기반 로그인 API를 구현합니다.")
            );

            RecordedRequest repoRequest = server.takeRequest();
            assertThat(repoRequest.getMethod()).isEqualTo("GET");
            assertThat(repoRequest.getPath()).isEqualTo("/repos/Markoalahub/Fithub_BE");
            assertThat(repoRequest.getHeader("Authorization")).isEqualTo("Bearer gho_test_token");

            RecordedRequest issueRequest = server.takeRequest();
            assertThat(issueRequest.getMethod()).isEqualTo("POST");
            assertThat(issueRequest.getPath()).isEqualTo("/repos/Markoalahub/Fithub_BE/issues");
            assertThat(issueRequest.getHeader("Authorization")).isEqualTo("Bearer gho_test_token");
            String issueRequestBody = issueRequest.getBody().readUtf8();
            assertThat(issueRequestBody).contains("\"title\":\"로그인 API 구현\"");
            assertThat(issueRequestBody).contains("\"body\":\"JWT 기반 로그인 API를 구현합니다.\"");

            assertThat(response.repoId()).isEqualTo(10L);
            assertThat(response.pipelineId()).isEqualTo(33L);
            assertThat(response.githubIssueNumber()).isEqualTo(42);
            assertThat(response.githubIssueUrl()).isEqualTo("https://github.com/Markoalahub/Fithub_BE/issues/42");
            assertThat(response.title()).isEqualTo("로그인 API 구현");
            assertThat(response.body()).isEqualTo("JWT 기반 로그인 API를 구현합니다.");
            assertThat(response.state()).isEqualTo("open");
            verify(userService).findById(1L);
            verify(pipelineV3Client).getPipeline(33L);
        }
    }

    @Test
    @DisplayName("GitHub access token이 없으면 400 예외를 반환하고 GitHub API를 호출하지 않는다")
    void createIssue_ThrowsBadRequestWhenGithubTokenMissing() {
        UserService userService = mock(UserService.class);
        PipelineV3Client pipelineV3Client = mock(PipelineV3Client.class);
        User user = new User(1L, "developer", "dev", "dev@test.com", "social", true, null, null, null, null);
        GithubWebClientService webClientService = new GithubWebClientService(userService);
        RepositoryIssueService repositoryIssueService = new RepositoryIssueService(userService, webClientService, pipelineV3Client);

        when(userService.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> repositoryIssueService.createIssue(
                1L,
                33L,
                new RepositoryIssueCreateRequest("로그인 API 구현", "본문")
        )).isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getReason()).contains("GitHub access token이 저장되지 않았습니다.");
        });

        verify(userService).findById(1L);
        verifyNoInteractions(pipelineV3Client);
    }

    @Test
    @DisplayName("파이썬 서버에 파이프라인 조회 결과가 없으면 404 예외를 반환하고 GitHub API를 호출하지 않는다")
    void createIssue_ThrowsNotFoundWhenPythonPipelineResponseNotFound() {
        UserService userService = mock(UserService.class);
        PipelineV3Client pipelineV3Client = mock(PipelineV3Client.class);
        User user = new User(1L, "developer", "dev", "dev@test.com", "social", true, "gho_test_token", null, null, null);
        GithubWebClientService webClientService = new GithubWebClientService(userService);
        RepositoryIssueService repositoryIssueService = new RepositoryIssueService(userService, webClientService, pipelineV3Client);

        when(userService.findById(1L)).thenReturn(Optional.of(user));
        when(pipelineV3Client.getPipeline(99L)).thenThrow(HttpClientErrorException.create(
                HttpStatus.NOT_FOUND,
                "Not Found",
                null,
                null,
                null
        ));

        assertThatThrownBy(() -> repositoryIssueService.createIssue(
                1L,
                99L,
                new RepositoryIssueCreateRequest("로그인 API 구현", "본문")
        )).isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(ex.getReason()).isEqualTo("파이프라인 조회 결과가 없습니다.");
        });

        verify(userService).findById(1L);
        verify(pipelineV3Client).getPipeline(99L);
    }

    @Test
    @DisplayName("파이프라인에 연결된 GitHub repository URL이 없으면 400 예외를 반환하고 GitHub API를 호출하지 않는다")
    void createIssue_ThrowsBadRequestWhenPipelineRepositoryMissing() {
        UserService userService = mock(UserService.class);
        PipelineV3Client pipelineV3Client = mock(PipelineV3Client.class);
        User user = new User(1L, "developer", "dev", "dev@test.com", "social", true, "gho_test_token", null, null, null);

        when(userService.findById(1L)).thenReturn(Optional.of(user));
        when(pipelineV3Client.getPipeline(33L)).thenReturn(new PipelineV3Response(
                33L,
                1L,
                "BE",
                1,
                "Spring Boot",
                null,
                List.of()
        ));

        GithubWebClientService webClientService = new GithubWebClientService(userService);
        RepositoryIssueService repositoryIssueService = new RepositoryIssueService(userService, webClientService, pipelineV3Client);

        assertThatThrownBy(() -> repositoryIssueService.createIssue(
                1L,
                33L,
                new RepositoryIssueCreateRequest("로그인 API 구현", "본문")
        )).isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getReason()).contains("파이프라인에 연결된 GitHub repository URL이 없습니다.");
        });

        verify(userService).findById(1L);
        verify(pipelineV3Client).getPipeline(33L);
    }
}
