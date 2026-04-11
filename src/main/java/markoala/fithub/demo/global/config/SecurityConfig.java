package markoala.fithub.demo.global.config;

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
import markoala.fithub.demo.global.security.jwt.JwtTokenProvider;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtTokenProvider tokenProvider;
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
                                .csrf(csrf -> csrf.disable())
                                // OAuth2는 세션이 필요함. JWT와 세션을 혼용
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                                .authorizeHttpRequests(auth -> auth
                                                // 1. OAuth2 콜백 경로는 OAuth2 필터가 처리해야 하므로 명시적으로 허용
                                                .requestMatchers("/login/oauth2/code/**").permitAll()
                                                // 2. 공통 리소스, 로그인 페이지는 '무조건' 허용
                                                .requestMatchers("/", "/login/**", "/signup/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                                                .permitAll()
                                                .requestMatchers(PathRequest.toStaticResources().atCommonLocations())
                                                .permitAll()
                                                // 3. 그 외 모든 경로는 인증 필요
                                                .anyRequest().authenticated())
                                .oauth2Login(oauth2 -> oauth2
                                                .loginPage("/login")
                                                .authorizationEndpoint(auth -> auth
                                                        .baseUri("/oauth2/authorization"))
                                                .redirectionEndpoint(redirect -> redirect
                                                        .baseUri("/login/oauth2/code/*"))
                                                .successHandler(successHandler) // 성공 핸들러 등록
                                )
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/login")
                                                .invalidateHttpSession(true)
                                                .deleteCookies("JSESSIONID")
                                )
                                // JWT 필터 등록 (UsernamePasswordAuthenticationFilter 이전에 실행)
                                .addFilterBefore(new JwtAuthenticationFilter(tokenProvider),
                                                UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}
