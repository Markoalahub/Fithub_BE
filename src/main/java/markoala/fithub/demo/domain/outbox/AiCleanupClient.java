package markoala.fithub.demo.domain.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
public class AiCleanupClient {

    private static final Logger log = LoggerFactory.getLogger(AiCleanupClient.class);

    private final RestClient restClient;

    public AiCleanupClient(@Qualifier("fastApiRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public void deleteProjectResources(Long projectId) {
        try {
            restClient.delete()
                    .uri("/internal/projects/{projectId}", projectId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound e) {
            log.info("[AI Cleanup] Project resources already absent: projectId={}", projectId);
        }
    }
}
