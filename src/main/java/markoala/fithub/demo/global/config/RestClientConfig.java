package markoala.fithub.demo.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${fastapi.base-url:http://localhost:8000}")
    private String fastApiBaseUrl;

    @Bean(name = "fastApiRestClient")
    public RestClient fastApiRestClient() {
        return RestClient.builder()
                .baseUrl(fastApiBaseUrl)
                .build();
    }
}
