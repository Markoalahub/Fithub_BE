package markoala.fithub.demo.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 전역 WebMVC 설정 클래스
 * 
 * HTTP 응답 헤더에 Content-Type: application/json;charset=UTF-8이
 * 명시적으로 포함되도록 보장하여, 클라이언트/브라우저 측의 한글 인코딩 깨짐을 방지합니다.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.stream()
                .filter(converter -> converter instanceof MappingJackson2HttpMessageConverter)
                .forEach(converter -> {
                    MappingJackson2HttpMessageConverter jacksonConverter = (MappingJackson2HttpMessageConverter) converter;
                    
                    // 디폴트 Charset을 명시적으로 UTF-8로 고정
                    jacksonConverter.setDefaultCharset(StandardCharsets.UTF_8);
                    
                    // 응답 미디어 타입에 charset=UTF-8 강제 포함 설정
                    jacksonConverter.setSupportedMediaTypes(List.of(
                            new MediaType("application", "json", StandardCharsets.UTF_8),
                            new MediaType("application", "*+json", StandardCharsets.UTF_8)
                    ));
                });
    }
}
