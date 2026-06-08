package markoala.fithub.demo.domain.issue;

import markoala.fithub.demo.domain.github.dto.GithubRepositoryDto;
import markoala.fithub.demo.domain.github.service.GithubRepositoryService;
import markoala.fithub.demo.global.config.SecurityConfig;
import markoala.fithub.demo.global.security.jwt.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GithubRepositoryController.class)
@Import(SecurityConfig.class)
class GithubRepositoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GithubRepositoryService githubRepositoryService;

    @MockBean
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        when(jwtProvider.validateAccessToken(anyString())).thenReturn(true);
        when(jwtProvider.getUserIdFromToken(anyString())).thenReturn(1L);
    }

    @Test
    @DisplayName("현재 사용자의 GitHub access token으로 레포 목록을 조회한다")
    void getAvailableRepositories_UsesCurrentUserId() throws Exception {
        when(githubRepositoryService.getMyRepos(1L)).thenReturn(List.of(
                new GithubRepositoryDto(
                        10L,
                        "Fithub_BE",
                        "Markoalahub/Fithub_BE",
                        "https://github.com/Markoalahub/Fithub_BE",
                        "Spring server",
                        false,
                        3,
                        2,
                        "Java",
                        "2026-01-01T00:00:00Z",
                        "2026-06-05T00:00:00Z",
                        "main",
                        "2026-06-05T01:00:00Z",
                        "https://github.com/Markoalahub/Fithub_BE.git"
                )
        ));

        mockMvc.perform(get("/repositories")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.repositories[0].repo_id").value(10L))
                .andExpect(jsonPath("$.repositories[0].repo_name").value("Fithub_BE"))
                .andExpect(jsonPath("$.repositories[0].repo_url_name").value("Markoalahub/Fithub_BE"))
                .andExpect(jsonPath("$.repositories[0].repo_url").value("https://github.com/Markoalahub/Fithub_BE"))
                .andExpect(jsonPath("$.repositories[0].starCount").value(3))
                .andExpect(jsonPath("$.repositories[0].language").value("Java"))
                .andExpect(jsonPath("$.repositories[0].id").doesNotExist())
                .andExpect(jsonPath("$.repositories[0].name").doesNotExist())
                .andExpect(jsonPath("$.repositories[0].fullName").doesNotExist())
                .andExpect(jsonPath("$.repositories[0].htmlUrl").doesNotExist())
                .andExpect(jsonPath("$.repositories[0].stargazersCount").doesNotExist());

        verify(githubRepositoryService).getMyRepos(1L);
    }

    @Test
    @DisplayName("현재 사용자의 GitHub access token으로 레포 단일 정보를 조회한다")
    void getRepositoryDetail_UsesCurrentUserIdAndRepoId() throws Exception {
        when(githubRepositoryService.getRepoDetail(1L, 10L)).thenReturn(
                new GithubRepositoryDto(
                        10L,
                        "Fithub_BE",
                        "Markoalahub/Fithub_BE",
                        "https://github.com/Markoalahub/Fithub_BE",
                        "Spring server",
                        false,
                        3,
                        2,
                        "Java",
                        "2026-01-01T00:00:00Z",
                        "2026-06-05T00:00:00Z",
                        "main",
                        "2026-06-05T01:00:00Z",
                        "https://github.com/Markoalahub/Fithub_BE.git"
                )
        );

        mockMvc.perform(get("/repositories/10")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repo_id").value(10L))
                .andExpect(jsonPath("$.repo_name").value("Fithub_BE"))
                .andExpect(jsonPath("$.repo_url_name").value("Markoalahub/Fithub_BE"))
                .andExpect(jsonPath("$.repo_url").value("https://github.com/Markoalahub/Fithub_BE"))
                .andExpect(jsonPath("$.starCount").value(3))
                .andExpect(jsonPath("$.defaultBranch").value("main"))
                .andExpect(jsonPath("$.updatedAt").value("2026-06-05T00:00:00Z"))
                .andExpect(jsonPath("$.pushedAt").value("2026-06-05T01:00:00Z"))
                .andExpect(jsonPath("$.cloneUrl").value("https://github.com/Markoalahub/Fithub_BE.git"));

        verify(githubRepositoryService).getRepoDetail(1L, 10L);
    }
}
