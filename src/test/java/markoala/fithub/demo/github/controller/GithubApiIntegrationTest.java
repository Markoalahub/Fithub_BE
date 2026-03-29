package markoala.fithub.demo.github.controller;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Client;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class GithubApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static MockWebServer mockWebServer;

    @BeforeAll
    static void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // Dispatcher를 이용해 GitHub API의 다중 호출(Repository 조회, Rate Limit 조회, Issue 조회) 경로를 분기 처리
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath();
                if (path == null) return new MockResponse().setResponseCode(404);

                if (path.contains("/rate_limit")) {
                    return new MockResponse()
                            .setBody("{\"resources\":{\"core\":{\"limit\":5000,\"remaining\":4999,\"reset\":1372700873}}}")
                            .addHeader("Content-Type", "application/json;charset=UTF-8");
                } else if (path.equals("/repos/mockowner/mockrepo")) {
                    return new MockResponse()
                            .setBody("{\"id\": 12345, \"name\": \"mockrepo\", \"full_name\": \"mockowner/mockrepo\"}")
                            .addHeader("Content-Type", "application/json;charset=UTF-8");
                } else if (path.contains("/repos/mockowner/mockrepo/issues")) {
                    // 한글 이슈 데이터 모킹 (Mock Response)
                    String issuesJson = """
                            [
                              {
                                "number": 42,
                                "title": "한글 이슈 테스트 제목",
                                "body": "본문 내용도 한글입니다.",
                                "state": "open",
                                "created_at": "2024-03-29T10:00:00Z",
                                "assignees": [],
                                "labels": [],
                                "milestone": null
                              }
                            ]
                            """;
                    return new MockResponse()
                            .setBody(issuesJson)
                            .addHeader("Content-Type", "application/json;charset=UTF-8");
                }
                return new MockResponse().setResponseCode(404);
            }
        });
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        // GithubApiServiceImpl의 @Value("${github.api.url}")에 MockWebServer URL을 동적 주입
        registry.add("github.api.url", () -> mockWebServer.url("/").toString());
    }

    @Test
    @DisplayName("이슈 목록 조회 API - 한글 인코딩 및 OAuth2 인증이 포함된 통합 테스트")
    void getIssues_ShouldReturnKoreanCharactersCorrectly() throws Exception {
        // given
        String owner = "mockowner";
        String repo = "mockrepo";

        // when & then
        mockMvc.perform(get("/api/v1/github/repos/{owner}/{repo}/issues", owner, repo)
                // oauth2Login(): 접근 권한(인증) 모킹 -> 302 리다이렉트 방지 및 200 OK 보장
                .with(oauth2Login())
                // oauth2Client("github"): 파라미터로 주입되는 @RegisteredOAuth2AuthorizedClient 모킹
                .with(oauth2Client("github"))
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print()) // 콘솔 출력 (디버깅)
                .andExpect(status().isOk())
                // 한글 인코딩이 깨지지 않고 온전히 반환되는지 검증
                .andExpect(content().contentTypeCompatibleWith("application/json;charset=UTF-8"))
                // DTO 필드 매핑 검증
                .andExpect(jsonPath("$[0].number").value(42))
                .andExpect(jsonPath("$[0].title").value("한글 이슈 테스트 제목"))
                .andExpect(jsonPath("$[0].body").value("본문 내용도 한글입니다."))
                .andExpect(jsonPath("$[0].state").value("open"));
    }
}
