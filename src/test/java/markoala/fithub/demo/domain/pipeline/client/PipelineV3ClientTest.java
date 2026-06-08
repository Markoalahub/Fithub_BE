package markoala.fithub.demo.domain.pipeline.client;

import markoala.fithub.demo.domain.pipeline.dto.request.PipelineGithubRepositoryUpdateRequest;
import markoala.fithub.demo.domain.pipeline.dto.response.PipelineV3Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

class PipelineV3ClientTest {

    @Test
    @DisplayName("GitHub repository URL 연결은 WebClient로 PATCH 요청을 보낸다")
    void updatePipelineGithubRepository_SendsPatchRequest() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .setBody("""
                            {
                              "pipe_id": 37,
                              "project_id": 1,
                              "category": "BE",
                              "version": 1,
                              "tech_stack": "Spring Boot",
                              "github_repo_url": "https://github.com/Markoalahub/Fithub_BE",
                              "feats": []
                            }
                            """));
            server.start();

            PipelineV3Client client = new PipelineV3Client(
                    RestClient.builder().baseUrl("http://localhost:0").build(),
                    server.url("/").toString()
            );

            PipelineV3Response response = client.updatePipelineGithubRepository(
                    37L,
                    new PipelineGithubRepositoryUpdateRequest("https://github.com/Markoalahub/Fithub_BE")
            );

            RecordedRequest request = server.takeRequest();
            assertThat(request.getMethod()).isEqualTo("PATCH");
            assertThat(request.getPath()).isEqualTo("/pipelines/37/github-repository");
            assertThat(request.getBody().readUtf8()).contains("\"github_repo_url\":\"https://github.com/Markoalahub/Fithub_BE\"");
            assertThat(response.id()).isEqualTo(37L);
            assertThat(response.githubRepoUrl()).isEqualTo("https://github.com/Markoalahub/Fithub_BE");
        }
    }
}
