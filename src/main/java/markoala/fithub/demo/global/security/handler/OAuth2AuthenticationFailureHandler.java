package markoala.fithub.demo.global.security.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationFailureHandler.class);

    private final String frontendOAuthCallbackUrl;

    public OAuth2AuthenticationFailureHandler(
            @Value("${app.frontend.oauth-callback-url:http://localhost:3000/auth/oauth/callback}") String frontendOAuthCallbackUrl
    ) {
        this.frontendOAuthCallbackUrl = frontendOAuthCallbackUrl;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        String provider = resolveProvider(request);
        String errorCode = exception.getClass().getSimpleName();
        String message = exception.getMessage() != null ? exception.getMessage() : "OAuth2 authentication failed";

        log.warn("[OAuth2FailureHandler] provider={}, error={}, message={}", provider, errorCode, message);

        String redirectUrl = UriComponentsBuilder.fromUriString(frontendOAuthCallbackUrl)
                .queryParam("success", "false")
                .queryParam("provider", provider)
                .queryParam("error", errorCode)
                .queryParam("message", message)
                .build()
                .encode()
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    private String resolveProvider(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        if (requestUri != null && !requestUri.isBlank()) {
            String[] segments = requestUri.split("/");
            for (int i = segments.length - 1; i >= 0; i--) {
                String segment = segments[i];
                if ("github".equalsIgnoreCase(segment) || "kakao".equalsIgnoreCase(segment)) {
                    return segment.toLowerCase();
                }
            }
        }

        String provider = request.getParameter("provider");
        if (provider != null && !provider.isBlank()) {
            return provider.toLowerCase();
        }

        return "unknown";
    }
}
