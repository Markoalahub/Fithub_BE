package markoala.fithub.demo.global.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;
import markoala.fithub.demo.global.security.handler.OAuth2SuccessHandler;
import markoala.fithub.demo.global.security.jwt.JwtAuthenticationFilter;
import markoala.fithub.demo.global.security.jwt.JwtProvider;
import markoala.fithub.demo.global.security.jwt.JwtTokenProvider;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtTokenProvider tokenProvider;
        private final JwtProvider jwtProvider;
        private final OAuth2SuccessHandler successHandler;

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
                                                // Swagger, 인증 관련
                                                .requestMatchers(
                                                        "/", "/login/**", "/signup/**",
                                                        "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                                                        "/api/v1/auth/token"
                                                ).permitAll()
                                                // 정적 리소스
                                                .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                                                // API 전체 허용 (개발 단계 - 추후 인증 필요 시 아래 주석 해제)
                                                .requestMatchers("/api/v1/**").permitAll()
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
}
