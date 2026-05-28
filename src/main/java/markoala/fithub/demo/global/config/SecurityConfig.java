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
import markoala.fithub.demo.global.security.handler.OAuth2AuthenticationFailureHandler;
import markoala.fithub.demo.global.security.handler.OAuth2SuccessHandler;
import markoala.fithub.demo.global.security.jwt.JwtAuthenticationFilter;
import markoala.fithub.demo.global.security.jwt.JwtProvider;

import java.util.Arrays;
import java.util.List;


@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtProvider jwtProvider;
        private final OAuth2SuccessHandler successHandler;
        private final OAuth2AuthenticationFailureHandler failureHandler;
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
                                                // OAuth2 콜백
                                                .requestMatchers("/login/oauth2/code/**").permitAll()
                                                .requestMatchers("/oauth2/**").permitAll()
                                                // Swagger, 인증 관련
                                                .requestMatchers(
                                                        "/", "/login/**", "/signup/**",
                                                        "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                                                        "/api/v1/auth/token",
                                                        "/api/v1/auth/dev/token",  // [DEV ONLY] 운영 시 제거
                                                        "/api/v1/auth/github/callback",
                                                        "/api/v1/auth/kakao/callback",
                                                        "/api/v1/auth/login",
                                                        "/api/v1/auth/kakao/login",
                                                        "/api/v1/auth/signup"
                                                ).permitAll()
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                                // 정적 리소스
                                                .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                                                // 인증이 필요한 API (JWT 토큰 필수)
                                                .requestMatchers("/api/v1/projects/**").authenticated()
                                                .requestMatchers("/api/v1/issues/**").authenticated()
                                                // 나머지 API는 개발 단계에서 허용
                                                // .requestMatchers("/api/v1/**").permitAll()
                                                // 그 외 모든 경로는 인증 필요
                                                .anyRequest().authenticated())
                                // API 경로는 리다이렉트 대신 401 JSON 반환
                                .exceptionHandling(ex -> ex
                                                .defaultAuthenticationEntryPointFor(
                                                        (request, response, authException) -> {
                                                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                                                response.setContentType("application/json;charset=UTF-8");
                                                                response.getWriter().write(
                                                                        "{\"error\":\"Unauthorized\",\"message\":\"" + authException.getMessage() + "\"}"
                                                                );
                                                        },
                                                        request -> request.getRequestURI().startsWith("/api/v1/")
                                                )
                                )
                                .oauth2Login(oauth2 -> oauth2
                                                .loginPage("/login")
                                                .authorizationEndpoint(auth -> auth
                                                        .baseUri("/oauth2/authorization"))
                                                .redirectionEndpoint(redirect -> redirect
                                                        .baseUri("/login/oauth2/code/*"))
                                                .successHandler(successHandler)
                                                .failureHandler(failureHandler)
                                )
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/login")
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
