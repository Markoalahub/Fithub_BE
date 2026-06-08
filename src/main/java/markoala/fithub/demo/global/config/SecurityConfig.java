package markoala.fithub.demo.global.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;
import markoala.fithub.demo.global.security.jwt.JwtAuthenticationFilter;
import markoala.fithub.demo.global.security.jwt.JwtProvider;

import java.util.Arrays;
import java.util.List;


@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtProvider jwtProvider;
        @Value("${app.frontend.allowed-origin-patterns:http://localhost:*,http://127.0.0.1:*}")
        private String allowedOriginPatterns;

        // 정적 리소스는 보안 필터를 적용하지 않음
        @Bean
        public WebSecurityCustomizer configure() {
                return web -> web.ignoring()
                                .requestMatchers(PathRequest
                                                .toStaticResources()
                                                .atCommonLocations());
        }

        // 보안 필터 체인 설정
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .csrf(csrf -> csrf
                                                .ignoringRequestMatchers("/h2-console/**") // H2 콘솔 CSRF 제외
                                                .disable())
                                // H2 콘솔은 iframe을 사용하므로 frameOptions 비활성화
                                .headers(headers -> headers
                                                .frameOptions(frame -> frame.disable()))
                                // OAuth2는 세션이 필요함. JWT와 세션을 혼용
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                                .authorizeHttpRequests(auth -> auth
                                                // H2 콘솔 허용
                                                .requestMatchers("/h2-console/**").permitAll()
                                                // Swagger, 인증 관련
                                                .requestMatchers(
                                                        "/",
                                                        "/health",
                                                        "/error",
                                                        "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                                                        "/auth/token",
                                                        "/auth/refresh",
                                                        "/auth/dev/token",  // [DEV ONLY] 운영 시 제거
                                                        "/auth/github/login",
                                                        "/auth/github/callback",
                                                        "/auth/kakao/callback",
                                                        "/auth/kakao/login",
                                                        "/auth/signup"
                                                ).permitAll()
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                                // 정적 리소스
                                                .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                                                // 인증이 필요한 API (JWT 토큰 필수)
                                                .requestMatchers("/projects/**").authenticated()
                                                .requestMatchers("/issues/**").authenticated()
                                                // 나머지 API는 개발 단계에서 허용
                                                // .requestMatchers("/**").permitAll()
                                                // 그 외 모든 경로는 인증 필요
                                                .anyRequest().authenticated())
                                // 미인증 요청은 401 JSON 반환 (리다이렉트 없음)
                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint(
                                                        (request, response, authException) -> {
                                                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                                                response.setContentType("application/json;charset=UTF-8");
                                                                response.getWriter().write(
                                                                        "{\"error\":\"Unauthorized\",\"message\":\"" + authException.getMessage() + "\"}"
                                                                );
                                                        }
                                                )
                                )
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessHandler((request, response, authentication) -> {
                                                        response.setStatus(HttpServletResponse.SC_OK);
                                                        response.setContentType("application/json;charset=UTF-8");
                                                        response.getWriter().write(
                                                                "{\"success\":true,\"message\":\"Logged out successfully\"}"
                                                        );
                                                })
                                                .invalidateHttpSession(true)
                                                .deleteCookies("JSESSIONID")
                                )
                                // JWT 필터 등록
                                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider),
                                                UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOriginPatterns(
                        Arrays.stream(allowedOriginPatterns.split(","))
                                .map(String::trim)
                                .filter(origin -> !origin.isEmpty())
                                .toList()
                );
                configuration.setAllowedMethods(
                        List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD")
                );
                configuration.setAllowedHeaders(List.of("*"));
                configuration.setExposedHeaders(List.of("Authorization", "Content-Type"));
                configuration.setAllowCredentials(true);
                configuration.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }
}
