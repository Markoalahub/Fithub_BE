package markoala.fithub.demo.github.dto;

import java.time.Instant;

/**
 * GitHub API Rate Limit 정보 DTO
 *
 * @param limit     시간당 최대 요청 횟수 (인증 사용자: 5,000)
 * @param remaining 남은 요청 가능 횟수
 * @param resetAt   쿼타 리셋 시각 (UTC)
 */
public record RateLimitResponse(
        int limit,
        int remaining,
        Instant resetAt
) {}
