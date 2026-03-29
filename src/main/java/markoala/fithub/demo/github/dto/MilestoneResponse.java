package markoala.fithub.demo.github.dto;

/**
 * 마일스톤 응답 DTO
 *
 * @param number      마일스톤 번호 (이슈 할당에 사용)
 * @param title       마일스톤 제목
 * @param description 마일스톤 설명
 * @param state       마일스톤 상태 (open/closed)
 * @param dueOn       마감일 (ISO 8601 형식)
 * @param htmlUrl     GitHub 웹 URL
 */
public record MilestoneResponse(
        int number,
        String title,
        String description,
        String state,
        String dueOn,
        String htmlUrl
) {}
