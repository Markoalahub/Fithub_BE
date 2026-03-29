package markoala.fithub.demo.github.service;

import lombok.extern.slf4j.Slf4j;
import markoala.fithub.demo.github.dto.*;
import markoala.fithub.demo.global.exception.GithubApiExecutionException;
import org.kohsuke.github.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * GitHub API 서비스 구현체 (org.kohsuke.github 라이브러리 사용)
 *
 * <p><b>보안 아키텍처</b>: 모든 메서드는 컨트롤러에서 추출한 accessToken을 파라미터로 전달받습니다.
 * 서비스 내에서 직접 DB를 조회하여 토큰을 가져오지 않습니다.</p>
 *
 * <p><b>모니터링</b>: 모든 API 호출 전후에 Rate Limit 잔여 쿼타를 SLF4J 로그로 기록합니다.</p>
 */
@Service
@Slf4j
public class GithubApiServiceImpl implements GithubApiService {

    @Value("${github.api.url:https://api.github.com}")
    private String githubApiUrl;

    // ──────────────────────────────────────────────────────────────
    // GitHub 클라이언트 팩토리
    // ──────────────────────────────────────────────────────────────

    /**
     * 주어진 accessToken으로 GitHub 클라이언트를 생성합니다.
     * 호출마다 새로 생성하여 요청 간 토큰 오염을 방지합니다.
     */
    private GitHub createGitHubClient(String accessToken) {
        try {
            return new GitHubBuilder()
                    .withOAuthToken(accessToken)
                    .withEndpoint(githubApiUrl)
                    .build();
        } catch (IOException e) {
            throw new GithubApiExecutionException(
                    "GitHub 클라이언트 생성에 실패했습니다.", e, HttpStatus.UNAUTHORIZED
            );
        }
    }

    /**
     * 저장소를 조회합니다.
     */
    private GHRepository getRepository(GitHub github, String owner, String repo) {
        try {
            return github.getRepository(owner + "/" + repo);
        } catch (IOException e) {
            throw new GithubApiExecutionException(
                    String.format("저장소 '%s/%s'를 찾을 수 없습니다.", owner, repo),
                    e, HttpStatus.NOT_FOUND
            );
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Rate Limit 로깅
    // ──────────────────────────────────────────────────────────────

    /**
     * GitHub API의 현재 Rate Limit 정보를 로그로 출력합니다.
     * 시간당 5,000회 제한을 효율적으로 관리하기 위한 모니터링 목적입니다.
     *
     * @param github GitHub 클라이언트
     * @param phase  호출 단계 (BEFORE / AFTER)
     * @param action 수행 중인 작업 설명
     */
    private void logRateLimit(GitHub github, String phase, String action) {
        try {
            GHRateLimit rateLimit = github.getRateLimit();
            GHRateLimit.Record core = rateLimit.getCore();
            log.info("[GitHub Rate Limit] [{}] {} — 남은 요청: {}/{}, 리셋 시각: {}",
                    phase, action, core.getRemaining(), core.getLimit(), core.getResetDate());
        } catch (IOException e) {
            log.warn("[GitHub Rate Limit] Rate Limit 정보 조회 실패: {}", e.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 마일스톤 생성
    // ──────────────────────────────────────────────────────────────

    @Override
    public MilestoneResponse createMilestone(String accessToken, String owner, String repo, MilestoneRequest request) {
        GitHub github = createGitHubClient(accessToken);
        GHRepository repository = getRepository(github, owner, repo);

        String action = String.format("마일스톤 생성 [%s] in %s/%s", request.title(), owner, repo);
        logRateLimit(github, "BEFORE", action);

        try {
            // LocalDate → ISO 8601 형식 (YYYY-MM-DDTHH:MM:SSZ) 변환
            Date dueDate = Date.from(
                    request.dueDate().atStartOfDay(ZoneOffset.UTC).toInstant()
            );

            GHMilestone milestone = repository.createMilestone(
                    request.title(),
                    request.description()
            );
            milestone.setDueOn(dueDate);

            logRateLimit(github, "AFTER", action);

            return toMilestoneResponse(milestone);

        } catch (IOException e) {
            throw new GithubApiExecutionException(
                    String.format("마일스톤 '%s' 생성에 실패했습니다: %s", request.title(), e.getMessage()), e
            );
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 이슈 생성 및 마일스톤 할당
    // ──────────────────────────────────────────────────────────────

    @Override
    public IssueResponse createIssue(String accessToken, String owner, String repo, IssueRequest request) {
        GitHub github = createGitHubClient(accessToken);
        GHRepository repository = getRepository(github, owner, repo);

        String action = String.format("이슈 생성 [%s] → 마일스톤 #%d in %s/%s",
                request.title(), request.milestoneNumber(), owner, repo);
        logRateLimit(github, "BEFORE", action);

        try {
            // 마일스톤 조회
            GHMilestone milestone = repository.getMilestone(request.milestoneNumber());

            // 이슈 빌더 구성
            GHIssueBuilder builder = repository.createIssue(request.title())
                    .body(request.body())
                    .milestone(milestone);

            // 라벨 추가
            if (request.labels() != null && !request.labels().isEmpty()) {
                for (String label : request.labels()) {
                    builder.label(label);
                }
            }

            // 담당자 추가
            if (request.assignees() != null && !request.assignees().isEmpty()) {
                for (String assignee : request.assignees()) {
                    builder.assignee(assignee);
                }
            }

            GHIssue issue = builder.create();

            logRateLimit(github, "AFTER", action);

            return toIssueResponse(issue);

        } catch (IOException e) {
            throw new GithubApiExecutionException(
                    String.format("이슈 '%s' 생성에 실패했습니다: %s", request.title(), e.getMessage()), e
            );
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 이슈 목록 조회
    // ──────────────────────────────────────────────────────────────

    @Override
    public List<IssueResponse> getIssues(String accessToken, String owner, String repo) {
        GitHub github = createGitHubClient(accessToken);
        GHRepository repository = getRepository(github, owner, repo);

        String action = String.format("이슈 목록 조회 %s/%s", owner, repo);
        logRateLimit(github, "BEFORE", action);

        try {
            List<GHIssue> issues = repository.getIssues(GHIssueState.ALL);

            List<IssueResponse> result = issues.stream()
                    .map(this::toIssueResponse)
                    .collect(Collectors.toList());

            logRateLimit(github, "AFTER", action);

            return result;

        } catch (IOException e) {
            throw new GithubApiExecutionException(
                    String.format("이슈 목록 조회에 실패했습니다 (%s/%s): %s", owner, repo, e.getMessage()), e
            );
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 이슈 타임라인 (생성/종료 시간) 조회
    // ──────────────────────────────────────────────────────────────

    @Override
    public List<IssueTimelineResponse> getTimeline(String accessToken, String owner, String repo) {
        GitHub github = createGitHubClient(accessToken);
        GHRepository repository = getRepository(github, owner, repo);

        String action = String.format("이슈 타임라인 조회 %s/%s", owner, repo);
        logRateLimit(github, "BEFORE", action);

        try {
            List<GHIssue> issues = repository.getIssues(GHIssueState.ALL);

            List<IssueTimelineResponse> result = issues.stream()
                    .map(this::toTimelineResponse)
                    .collect(Collectors.toList());

            logRateLimit(github, "AFTER", action);

            return result;

        } catch (IOException e) {
            throw new GithubApiExecutionException(
                    String.format("타임라인 조회에 실패했습니다 (%s/%s): %s", owner, repo, e.getMessage()), e
            );
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Rate Limit 직접 조회
    // ──────────────────────────────────────────────────────────────

    @Override
    public RateLimitResponse getRateLimit(String accessToken) {
        GitHub github = createGitHubClient(accessToken);

        try {
            GHRateLimit rateLimit = github.getRateLimit();
            GHRateLimit.Record core = rateLimit.getCore();

            log.info("[GitHub Rate Limit] 직접 조회 — 남은 요청: {}/{}, 리셋 시각: {}",
                    core.getRemaining(), core.getLimit(), core.getResetDate());

            return new RateLimitResponse(
                    core.getLimit(),
                    core.getRemaining(),
                    core.getResetDate().toInstant()
            );

        } catch (IOException e) {
            throw new GithubApiExecutionException(
                    "Rate Limit 정보 조회에 실패했습니다: " + e.getMessage(), e
            );
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 엔티티 → DTO 변환 헬퍼
    // ──────────────────────────────────────────────────────────────

    private MilestoneResponse toMilestoneResponse(GHMilestone milestone) {
        try {
            String dueOnFormatted = null;
            Date dueOn = milestone.getDueOn();
            if (dueOn != null) {
                dueOnFormatted = dueOn.toInstant().atZone(ZoneOffset.UTC)
                        .format(DateTimeFormatter.ISO_INSTANT);
            }

            return new MilestoneResponse(
                    milestone.getNumber(),
                    milestone.getTitle(),
                    milestone.getDescription(),
                    milestone.getState().name().toLowerCase(),
                    dueOnFormatted,
                    milestone.getHtmlUrl() != null ? milestone.getHtmlUrl().toString() : null
            );
        } catch (Exception e) {
            throw new GithubApiExecutionException("마일스톤 응답 변환에 실패했습니다.", e);
        }
    }

    private IssueResponse toIssueResponse(GHIssue issue) {
        try {
            GHMilestone milestone = issue.getMilestone();
            return new IssueResponse(
                    issue.getNumber(),
                    issue.getTitle(),
                    issue.getBody(),
                    issue.getState().name().toLowerCase(),
                    milestone != null ? milestone.getNumber() : null,
                    milestone != null ? milestone.getTitle() : null,
                    issue.getLabels().stream()
                            .map(GHLabel::getName)
                            .collect(Collectors.toList()),
                    issue.getAssignees().stream()
                            .map(GHUser::getLogin)
                            .collect(Collectors.toList()),
                    issue.getCreatedAt() != null
                            ? issue.getCreatedAt().toInstant().toString()
                            : null,
                    issue.getClosedAt() != null
                            ? issue.getClosedAt().toInstant().toString()
                            : null,
                    issue.getHtmlUrl() != null ? issue.getHtmlUrl().toString() : null
            );
        } catch (IOException e) {
            throw new GithubApiExecutionException("이슈 응답 변환에 실패했습니다.", e);
        }
    }

    private IssueTimelineResponse toTimelineResponse(GHIssue issue) {
        try {
            return new IssueTimelineResponse(
                    issue.getNumber(),
                    issue.getTitle(),
                    issue.getState().name().toLowerCase(),
                    issue.getCreatedAt() != null
                            ? issue.getCreatedAt().toInstant().toString()
                            : null,
                    issue.getClosedAt() != null
                            ? issue.getClosedAt().toInstant().toString()
                            : null,
                    issue.getUpdatedAt() != null
                            ? issue.getUpdatedAt().toInstant().toString()
                            : null
            );
        } catch (IOException e) {
            throw new GithubApiExecutionException("타임라인 응답 변환에 실패했습니다.", e);
        }
    }
}
