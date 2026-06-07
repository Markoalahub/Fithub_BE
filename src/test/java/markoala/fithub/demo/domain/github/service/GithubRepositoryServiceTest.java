package markoala.fithub.demo.domain.github.service;

import markoala.fithub.demo.domain.auth.GithubWebClientService;
import markoala.fithub.demo.domain.github.dto.GithubRepositoryDto;
import markoala.fithub.demo.domain.user.User;
import markoala.fithub.demo.domain.user.UserService;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GithubRepositoryServiceTest {

    @Test
    @DisplayName("userId로 사용자 GitHub access token을 조회해 GitHub 레포 목록을 호출한다")
    void getMyRepos_UsesUserGithubAccessToken() throws Exception {
        UserService userService = mock(UserService.class);
        User user = new User(
                1L,
                "developer",
                "dev",
                "dev@test.com",
                "social",
                true,
                "gho_test_token",
                null,
                null,
                null
        );
        when(userService.findById(1L)).thenReturn(Optional.of(user));

        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("""
                            [
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
                                "updated_at": "2026-06-05T00:00:00Z"
                              }
                            ]
                            """));
            server.start();

            GithubWebClientService webClientService = new GithubWebClientService(userService);
            ReflectionTestUtils.setField(webClientService, "githubApiBaseUrl", server.url("/").toString());
            GithubRepositoryService repositoryService = new GithubRepositoryService(webClientService, userService);

            List<GithubRepositoryDto> repos = repositoryService.getMyRepos(1L);

            RecordedRequest request = server.takeRequest();
            assertThat(request.getMethod()).isEqualTo("GET");
            assertThat(request.getPath()).startsWith("/user/repos");
            assertThat(request.getPath()).contains("affiliation=owner,collaborator,organization_member");
            assertThat(request.getHeader("Authorization")).isEqualTo("Bearer gho_test_token");
            assertThat(repos).hasSize(1);
            assertThat(repos.get(0).fullName()).isEqualTo("Markoalahub/Fithub_BE");
            verify(userService).findById(1L);
        }
    }

    @Test
    @DisplayName("userId와 repoId로 사용자 GitHub access token을 조회해 GitHub 레포 단일 정보를 호출한다")
    void getRepoDetail_UsesUserGithubAccessTokenAndRepoId() throws Exception {
        UserService userService = mock(UserService.class);
        User user = new User(
                1L,
                "developer",
                "dev",
                "dev@test.com",
                "social",
                true,
                "gho_test_token",
                null,
                null,
                null
        );
        when(userService.findById(1L)).thenReturn(Optional.of(user));

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
            server.start();

            GithubWebClientService webClientService = new GithubWebClientService(userService);
            ReflectionTestUtils.setField(webClientService, "githubApiBaseUrl", server.url("/").toString());
            GithubRepositoryService repositoryService = new GithubRepositoryService(webClientService, userService);

            GithubRepositoryDto repo = repositoryService.getRepoDetail(1L, 10L);

            RecordedRequest request = server.takeRequest();
            assertThat(request.getMethod()).isEqualTo("GET");
            assertThat(request.getPath()).isEqualTo("/repositories/10");
            assertThat(request.getHeader("Authorization")).isEqualTo("Bearer gho_test_token");
            assertThat(repo.id()).isEqualTo(10L);
            assertThat(repo.defaultBranch()).isEqualTo("main");
            assertThat(repo.cloneUrl()).isEqualTo("https://github.com/Markoalahub/Fithub_BE.git");
            verify(userService).findById(1L);
        }
    }

    @Test
    @DisplayName("GitHub access token이 없으면 400 예외를 반환한다")
    void getMyRepos_ThrowsBadRequestWhenGithubTokenMissing() {
        UserService userService = mock(UserService.class);
        User user = new User(1L, "developer", "dev", "dev@test.com", "social", true, null, null, null, null);
        GithubWebClientService webClientService = new GithubWebClientService(userService);
        GithubRepositoryService repositoryService = new GithubRepositoryService(webClientService, userService);

        when(userService.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> repositoryService.getMyRepos(1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("GitHub access token이 저장되지 않았습니다.");

        verify(userService).findById(1L);
    }

    @Test
    @DisplayName("GitHub access token이 없으면 레포 단일 조회에서 400 예외를 반환한다")
    void getRepoDetail_ThrowsBadRequestWhenGithubTokenMissing() {
        UserService userService = mock(UserService.class);
        User user = new User(1L, "developer", "dev", "dev@test.com", "social", true, null, null, null, null);
        GithubWebClientService webClientService = new GithubWebClientService(userService);
        GithubRepositoryService repositoryService = new GithubRepositoryService(webClientService, userService);

        when(userService.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> repositoryService.getRepoDetail(1L, 10L))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getReason()).contains("GitHub access token이 저장되지 않았습니다.");
                });

        verify(userService).findById(1L);
    }

    @Test
    @DisplayName("사용자가 없으면 레포 단일 조회에서 404 예외를 반환한다")
    void getRepoDetail_ThrowsNotFoundWhenUserMissing() {
        UserService userService = mock(UserService.class);
        GithubWebClientService webClientService = new GithubWebClientService(userService);
        GithubRepositoryService repositoryService = new GithubRepositoryService(webClientService, userService);

        when(userService.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> repositoryService.getRepoDetail(1L, 10L))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(ex.getReason()).contains("사용자를 찾을 수 없습니다");
                });

        verify(userService).findById(1L);
    }

    @Test
    @DisplayName("GitHub 레포 단일 조회에서 GitHub 404는 레포 없음 404 예외로 변환한다")
    void getRepoDetail_ThrowsNotFoundWhenGithubRepoMissing() throws Exception {
        UserService userService = mock(UserService.class);
        User user = new User(1L, "developer", "dev", "dev@test.com", "social", true, "gho_test_token", null, null, null);
        when(userService.findById(1L)).thenReturn(Optional.of(user));

        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse()
                    .setResponseCode(404)
                    .setHeader("Content-Type", "application/json")
                    .setBody("""
                            {
                              "message": "Not Found"
                            }
                            """));
            server.start();

            GithubWebClientService webClientService = new GithubWebClientService(userService);
            ReflectionTestUtils.setField(webClientService, "githubApiBaseUrl", server.url("/").toString());
            GithubRepositoryService repositoryService = new GithubRepositoryService(webClientService, userService);

            assertThatThrownBy(() -> repositoryService.getRepoDetail(1L, 10L))
                    .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                        assertThat(ex.getReason()).isEqualTo("레포지토리를 찾을 수 없습니다.");
                    });

            RecordedRequest request = server.takeRequest();
            assertThat(request.getMethod()).isEqualTo("GET");
            assertThat(request.getPath()).isEqualTo("/repositories/10");
            assertThat(request.getHeader("Authorization")).isEqualTo("Bearer gho_test_token");
            verify(userService).findById(1L);
        }
    }
}
