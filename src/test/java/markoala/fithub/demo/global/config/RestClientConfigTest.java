package markoala.fithub.demo.global.config;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

class RestClientConfigTest {

    @Test
    @DisplayName("fastApiRestClient는 PATCH 요청을 지원한다")
    void fastApiRestClient_SupportsPatchMethod() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("{\"ok\":true}"));
            server.start();

            RestClientConfig config = new RestClientConfig();
            ReflectionTestUtils.setField(config, "fastApiBaseUrl", server.url("/").toString());
            RestClient restClient = config.fastApiRestClient();

            restClient.patch()
                    .uri("/pipelines/37/github-repository")
                    .body("{\"github_repo_url\":\"https://github.com/Markoalahub/Fithub_BE\"}")
                    .retrieve()
                    .toBodilessEntity();

            RecordedRequest request = server.takeRequest();
            assertThat(request.getMethod()).isEqualTo("PATCH");
            assertThat(request.getPath()).isEqualTo("/pipelines/37/github-repository");
        }
    }
}
