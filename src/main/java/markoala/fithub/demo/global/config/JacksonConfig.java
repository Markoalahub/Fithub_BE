package markoala.fithub.demo.global.config;

import com.fasterxml.jackson.core.json.JsonWriteFeature;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 전역 설정 클래스
 * 
 * 한글 등 Non-ASCII 문자가 \\uXXXX 형태로 유니코드 이스케이프 되는 것을 강제로 방지합니다.
 * application.yml 설정이 무시될 경우를 대비한 최상위 적용입니다.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> builder.featuresToDisable(JsonWriteFeature.ESCAPE_NON_ASCII);
    }
}
