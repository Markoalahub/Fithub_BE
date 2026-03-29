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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath();
                String method = request.getMethod();
                if (path == null) return new MockResponse().setResponseCode(404);

                MockResponse mockResponse = new MockResponse()
                        .addHeader("X-RateLimit-Limit", "5000")
                        .addHeader("X-RateLimit-Remaining", "4999")
                        .addHeader("X-RateLimit-Reset", "1372700873")
                        .addHeader("Content-Type", "application/json;charset=UTF-8");

                if (path.contains("/rate_limit")) {
                    return mockResponse.setBody("{\"resources\":{\"core\":{\"limit\":5000,\"remaining\":4999,\"reset\":1372700873}}}");
                } 
                if (path.equals("/repos/mockowner/mockrepo")) {
                    return mockResponse.setBody("{\"id\": 12345, \"name\": \"mockrepo\", \"full_name\": \"mockowner/mockrepo\"}");
                } 
                if (path.contains("/repos/mockowner/mockrepo/milestones")) {
                    return mockResponse.setBody("{\"number\": 1, \"title\": \"새로운 한글 마일스톤\", \"description\": \"마일스톤 설명\", \"state\": \"open\", \"due_on\": \"2024-12-31T00:00:00Z\"}");
                } 
                if (path.contains("/timeline")) {
                    return mockResponse.setBody("[{\"event\": \"closed\", \"created_at\": \"2024-03-29T12:00:00Z\"}]");
                } 
                if (path.contains("/repos/mockowner/mockrepo/issues")) {
                    if ("POST".equals(method)) {
                        return mockResponse.setBody("{\"number\": 43, \"title\": \"한글 이슈 생성\", \"body\": \"본문\", \"state\": \"open\", \"assignees\": [], \"labels\": []}");
                    } else {
                        return mockResponse.setBody("""
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
                                """);
                    }
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
        registry.add("github.api.url", () -> mockWebServer.url("/").toString());
    }

    @Test
    @DisplayName("이슈 목록 조회 API - 통합 테스트")
    void getIssues_ShouldReturnKoreanCharactersCorrectly() throws Exception {
        mockMvc.perform(get("/api/v1/github/repos/mockowner/mockrepo/issues")
                .with(oauth2Login()).with(oauth2Client("github"))
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$[0].number").value(42))
                .andExpect(jsonPath("$[0].title").value("한글 이슈 테스트 제목"))
                .andExpect(jsonPath("$[0].body").value("본문 내용도 한글입니다."));
    }

    @Test
    @DisplayName("이슈 생성 API - 통합 테스트")
    void createIssue_ShouldSucceed() throws Exception {
        String requestJson = "{\"title\": \"한글 이슈 생성\", \"body\": \"본문\", \"milestoneNumber\": 1, \"assignees\": [], \"labels\": []}";
        
        mockMvc.perform(post("/api/v1/github/repos/mockowner/mockrepo/issues")
                .with(oauth2Login()).with(oauth2Client("github"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("한글 이슈 생성"));
    }

    @Test
    @DisplayName("이슈 타임라인 조회 API - 통합 테스트")
    void getTimeline_ShouldSucceed() throws Exception {
        mockMvc.perform(get("/api/v1/github/repos/mockowner/mockrepo/timeline")
                .with(oauth2Login()).with(oauth2Client("github"))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].issueNumber").value(42))
                .andExpect(jsonPath("$[0].state").value("open"));
    }
}
