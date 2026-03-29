package markoala.fithub.demo.github.service;

import markoala.fithub.demo.github.dto.*;

import java.util.List;

/**
 * GitHub API 서비스 인터페이스
 *
 * <p>모든 메서드는 컨트롤러로부터 accessToken을 직접 파라미터로 전달받습니다.
 * 서비스 내에서 DB를 직접 조회하여 토큰을 가져오지 않습니다.</p>
 */
public interface GithubApiService {

    /**
     * 마일스톤을 생성합니다.
     *
     * @param accessToken GitHub OAuth2 액세스 토큰
     * @param owner       저장소 소유자
     * @param repo        저장소 이름
     * @param request     마일스톤 생성 요청 DTO (LocalDate 포함)
     * @return 생성된 마일스톤 응답
     */
    MilestoneResponse createMilestone(String accessToken, String owner, String repo, MilestoneRequest request);

    /**
     * 이슈를 생성하고 마일스톤에 할당합니다.
     *
     * @param accessToken GitHub OAuth2 액세스 토큰
     * @param owner       저장소 소유자
     * @param repo        저장소 이름
     * @param request     이슈 생성 요청 DTO (milestoneNumber 포함)
     * @return 생성된 이슈 응답
     */
    IssueResponse createIssue(String accessToken, String owner, String repo, IssueRequest request);

    /**
     * 저장소의 이슈 목록을 조회합니다.
     *
     * @param accessToken GitHub OAuth2 액세스 토큰
     * @param owner       저장소 소유자
     * @param repo        저장소 이름
     * @return 이슈 응답 목록
     */
    List<IssueResponse> getIssues(String accessToken, String owner, String repo);

    /**
     * 저장소 이슈들의 타임라인(생성/종료 시간) 데이터를 조회합니다.
     *
     * @param accessToken GitHub OAuth2 액세스 토큰
     * @param owner       저장소 소유자
     * @param repo        저장소 이름
     * @return 이슈 타임라인 응답 목록
     */
    List<IssueTimelineResponse> getTimeline(String accessToken, String owner, String repo);

    /**
     * 현재 GitHub API Rate Limit 정보를 조회합니다.
     *
     * @param accessToken GitHub OAuth2 액세스 토큰
     * @return Rate Limit 응답
     */
    RateLimitResponse getRateLimit(String accessToken);
}
