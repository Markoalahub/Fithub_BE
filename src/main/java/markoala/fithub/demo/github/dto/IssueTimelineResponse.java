package markoala.fithub.demo.github.dto;

/**
 * 이슈 타임라인(생성/종료 시간) 응답 DTO
 *
 * @param issueNumber 이슈 번호
 * @param title       이슈 제목
 * @param state       이슈 상태 (open/closed)
 * @param createdAt   생성일시 (ISO 8601)
 * @param closedAt    종료일시 (ISO 8601, null 가능)
 * @param updatedAt   최종 수정일시 (ISO 8601)
 */
public record IssueTimelineResponse(
        int issueNumber,
        String title,
        String state,
        String createdAt,
        String closedAt,
        String updatedAt
) {}
