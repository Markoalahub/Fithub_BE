package markoala.fithub.demo.global.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Fithub API",
                version = "1.0.0",
                description = """
                        🚀 Fithub — AI 기반 지능형 협업 허브
                        
                        기획자와 개발자 사이의 정보 비대칭성을 해결하는 MSA 기반 시스템입니다.
                        
                        **주요 기능:**
                        - AI 파이프라인 생성 (FastAPI)
                        - GitHub Issue 자동 생성 및 동기화
                        - 프로젝트 & 멤버 관리
                        - 회의록 요약 및 AI 분석
                        
                        **아키텍처:**
                        - Spring Boot 3.5.12 (Core API & Orchestration)
                        - FastAPI (AI & Data Processing)
                        - GitHub OAuth 2.0 (Authentication)
                        """,
                contact = @Contact(
                        name = "Fithub Team",
                        url = "https://github.com/markoala/fithub",
                        email = "support@fithub.io"
                )
        ),
        servers = {
                @Server(
                        url = "http://localhost:8080",
                        description = "Local Development Server"
                ),
                @Server(
                        url = "http://localhost:8000",
                        description = "FastAPI Integration Server"
                )
        }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "GitHub OAuth 2.0 Access Token (JWT Format)"
)
public class OpenAPIConfig {
}
