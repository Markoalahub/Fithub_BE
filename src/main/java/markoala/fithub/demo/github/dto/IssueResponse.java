package markoala.fithub.demo.github.dto;

import java.util.List;

/**
 * 이슈 응답 DTO
 *
 * @param number          이슈 번호
 * @param title           이슈 제목
 * @param body            이슈 본문
 * @param state           이슈 상태 (open/closed)
 * @param milestoneNumber 할당된 마일스톤 번호 (null 가능)
 * @param milestoneTitle  할당된 마일스톤 제목 (null 가능)
 * @param labels          라벨 목록
 * @param assignees       담당자 목록
 * @param createdAt       생성일시 (ISO 8601)
 * @param closedAt        종료일시 (ISO 8601, null 가능)
 * @param htmlUrl         GitHub 웹 URL
 */
public record IssueResponse(
        int number,
        String title,
        String body,
        String state,
        Integer milestoneNumber,
        String milestoneTitle,
        List<String> labels,
        List<String> assignees,
        String createdAt,
        String closedAt,
        String htmlUrl
) {}
