package markoala.fithub.demo.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, bearerAuthScheme()))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .info(apiInfo());
    }

    private SecurityScheme bearerAuthScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("로그인 콜백에서 발급한 서비스 JWT accessToken을 Authorization 헤더의 Bearer 토큰으로 사용합니다.");
    }

    private Info apiInfo() {
        return new Info()
                .title("Fithub API 명세서")
                .description("Fithub 백엔드 서버 API 명세서입니다. 인증이 필요한 API는 Authorization: Bearer {accessToken} 헤더를 사용합니다.")
                .version("v1.0.0");
    }
}
