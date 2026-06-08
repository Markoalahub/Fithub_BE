package markoala.fithub.demo.global.security.jwt;

import markoala.fithub.demo.domain.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private final JwtProvider jwtProvider = new JwtProvider(
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
            3600000,
            604800000
    );

    @Test
    @DisplayName("Access Token과 Refresh Token은 타입별로만 검증에 성공한다")
    void validateTokenByType() {
        User user = new User(1L, "github-user", "nick", "github@test.com", "12345", true, null, null, null, null);

        String accessToken = jwtProvider.generateAccessToken(user);
        String refreshToken = jwtProvider.generateRefreshToken(user);

        assertThat(jwtProvider.validateAccessToken(accessToken)).isTrue();
        assertThat(jwtProvider.validateRefreshToken(accessToken)).isFalse();
        assertThat(jwtProvider.validateRefreshToken(refreshToken)).isTrue();
        assertThat(jwtProvider.validateAccessToken(refreshToken)).isFalse();
        assertThat(jwtProvider.getUserIdFromToken(refreshToken)).isEqualTo(1L);
    }
}
