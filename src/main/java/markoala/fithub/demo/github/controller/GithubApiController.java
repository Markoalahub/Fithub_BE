package markoala.fithub.demo.github.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import markoala.fithub.demo.github.dto.*;
import markoala.fithub.demo.github.service.GithubApiService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * GitHub API 컨트롤러
 *
 * <p><b>보안 아키텍처</b>:
 * {@code @RegisteredOAuth2AuthorizedClient}를 통해 Spring Security가 관리하는
 * OAuth2 토큰을 추출하여, 서비스 메서드에 직접 전달합니다.
 * 서비스 레이어에서는 절대 DB에서 토큰을 직접 조회하지 않습니다.</p>
 */
@RestController
@RequestMapping("/api/v1/github")
@RequiredArgsConstructor
public class GithubApiController {

    private final GithubApiService githubApiService;

    // ──────────────────────────────────────────────────────────────
    // 토큰 추출 헬퍼
    // ──────────────────────────────────────────────────────────────

    /**
     * OAuth2AuthorizedClient로부터 accessToken 문자열을 추출합니다.
     */
    private String extractToken(OAuth2AuthorizedClient authorizedClient) {
        return authorizedClient.getAccessToken().getTokenValue();
    }

    // ──────────────────────────────────────────────────────────────
    // 마일스톤 API
    // ──────────────────────────────────────────────────────────────

    /**
     * 마일스톤을 생성합니다.
     *
     * @param authorizedClient Spring Security가 관리하는 OAuth2 인증 클라이언트
     * @param owner            저장소 소유자
     * @param repo             저장소 이름
     * @param request          마일스톤 생성 요청 (title, description, dueDate)
     */
    @PostMapping("/repos/{owner}/{repo}/milestones")
    public ResponseEntity<MilestoneResponse> createMilestone(
            @RegisteredOAuth2AuthorizedClient("github") OAuth2AuthorizedClient authorizedClient,
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestBody @Valid MilestoneRequest request) {

        MilestoneResponse response = githubApiService.createMilestone(
                extractToken(authorizedClient), owner, repo, request
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ──────────────────────────────────────────────────────────────
    // 이슈 API
    // ──────────────────────────────────────────────────────────────

    /**
     * 이슈를 생성하고 특정 마일스톤에 할당합니다.
     *
     * @param authorizedClient Spring Security가 관리하는 OAuth2 인증 클라이언트
     * @param owner            저장소 소유자
     * @param repo             저장소 이름
     * @param request          이슈 생성 요청 (title, body, milestoneNumber, labels, assignees)
     */
    @PostMapping("/repos/{owner}/{repo}/issues")
    public ResponseEntity<IssueResponse> createIssue(
            @RegisteredOAuth2AuthorizedClient("github") OAuth2AuthorizedClient authorizedClient,
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestBody @Valid IssueRequest request) {

        IssueResponse response = githubApiService.createIssue(
                extractToken(authorizedClient), owner, repo, request
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 저장소의 이슈 목록을 조회합니다.
     */
    @GetMapping("/repos/{owner}/{repo}/issues")
    public ResponseEntity<List<IssueResponse>> getIssues(
            @RegisteredOAuth2AuthorizedClient("github") OAuth2AuthorizedClient authorizedClient,
            @PathVariable String owner,
            @PathVariable String repo) {

        List<IssueResponse> response = githubApiService.getIssues(
                extractToken(authorizedClient), owner, repo
        );

        return ResponseEntity.ok(response);
    }

    // ──────────────────────────────────────────────────────────────
    // 타임라인 API
    // ──────────────────────────────────────────────────────────────

    /**
     * 저장소 이슈들의 타임라인(생성/종료 시간) 데이터를 조회합니다.
     */
    @GetMapping("/repos/{owner}/{repo}/timeline")
    public ResponseEntity<List<IssueTimelineResponse>> getTimeline(
            @RegisteredOAuth2AuthorizedClient("github") OAuth2AuthorizedClient authorizedClient,
            @PathVariable String owner,
            @PathVariable String repo) {

        List<IssueTimelineResponse> response = githubApiService.getTimeline(
                extractToken(authorizedClient), owner, repo
        );

        return ResponseEntity.ok(response);
    }

    // ──────────────────────────────────────────────────────────────
    // Rate Limit API
    // ──────────────────────────────────────────────────────────────

    /**
     * 현재 GitHub API Rate Limit 정보를 조회합니다.
     */
    @GetMapping("/rate-limit")
    public ResponseEntity<RateLimitResponse> getRateLimit(
            @RegisteredOAuth2AuthorizedClient("github") OAuth2AuthorizedClient authorizedClient) {

        RateLimitResponse response = githubApiService.getRateLimit(
                extractToken(authorizedClient)
        );

        return ResponseEntity.ok(response);
    }
}
