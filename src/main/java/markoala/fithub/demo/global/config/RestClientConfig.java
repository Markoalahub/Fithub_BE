package markoala.fithub.demo.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Value("${fastapi.base-url:http://localhost:8000}")
    private String fastApiBaseUrl;

    @Bean(name = "fastApiRestClient")
    public RestClient fastApiRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(30));
        factory.setReadTimeout(Duration.ofMinutes(10)); // AI 파이프라인 생성 시간 고려

        return RestClient.builder()
                .baseUrl(fastApiBaseUrl)
                .requestFactory(factory)
                .build();
    }
}
