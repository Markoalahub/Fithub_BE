package markoala.fithub.demo.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import markoala.fithub.demo.global.security.handler.OAuth2SuccessHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "GitHub OAuth2 로그인 및 JWT 발급 API")
public class AuthController {

    @GetMapping("/token")
    @Operation(
            summary = "JWT 토큰 발급",
            description = """
                    GitHub OAuth2 로그인 완료 후 JWT를 JSON으로 반환합니다.

                    **사용 흐름:**
                    1. 브라우저 또는 Postman에서 `/oauth2/authorization/github` 접속
                    2. GitHub 로그인 완료
                    3. 자동으로 이 엔드포인트로 redirect되며 JWT 반환
                    4. 이후 모든 API 요청에 `Authorization: Bearer <token>` 헤더 사용
                    """
    )
    @ApiResponse(responseCode = "200", description = "JWT 발급 성공")
    @ApiResponse(responseCode = "401", description = "OAuth2 로그인 먼저 필요")
    public ResponseEntity<Map<String, Object>> getToken(HttpSession session) {
        String token    = (String) session.getAttribute(OAuth2SuccessHandler.SESSION_KEY_TOKEN);
        Long   userId   = (Long)   session.getAttribute(OAuth2SuccessHandler.SESSION_KEY_USER_ID);
        String username = (String) session.getAttribute(OAuth2SuccessHandler.SESSION_KEY_USERNAME);

        if (token == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "OAuth2 로그인이 필요합니다",
                    "loginUrl", "/oauth2/authorization/github"
            ));
        }

        // 세션에서 제거 (1회용)
        session.removeAttribute(OAuth2SuccessHandler.SESSION_KEY_TOKEN);
        session.removeAttribute(OAuth2SuccessHandler.SESSION_KEY_USER_ID);
        session.removeAttribute(OAuth2SuccessHandler.SESSION_KEY_USERNAME);

        return ResponseEntity.ok(Map.of(
                "token",    token,
                "userId",   userId,
                "username", username,
                "type",     "Bearer"
        ));
    }
}
