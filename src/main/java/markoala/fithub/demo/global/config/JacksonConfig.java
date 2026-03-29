package markoala.fithub.demo.global.config;

import com.fasterxml.jackson.core.json.JsonWriteFeature;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> {
            // Spring Boot의 Jackson2ObjectMapperBuilder는 내부적으로
            // JsonWriteFeature 클래스를 Unknown feature class로 간주하여 Exception을 던질 수 있습니다.
            // 이를 우회하기 위해 builder.featuresToDisable() 대신, 
            // ObjectMapper가 생성된 직후에 Factory 설정을 직접 건드리는 postConfigurer를 사용합니다.
            builder.postConfigurer(objectMapper -> {
                objectMapper.getFactory().disable(JsonWriteFeature.ESCAPE_NON_ASCII.mappedFeature());
            });
        };
    }
}
