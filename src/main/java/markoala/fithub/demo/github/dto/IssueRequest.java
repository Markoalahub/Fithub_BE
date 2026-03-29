package markoala.fithub.demo.github.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 이슈 생성 요청 DTO
 *
 * @param title           이슈 제목
 * @param body            이슈 본문 (Markdown 지원)
 * @param milestoneNumber 할당할 마일스톤 번호
 * @param labels          이슈 라벨 목록 (선택)
 * @param assignees       담당자 GitHub 사용자명 목록 (선택)
 */
public record IssueRequest(
        @NotBlank(message = "이슈 제목은 필수입니다.")
        String title,

        String body,

        @NotNull(message = "마일스톤 번호는 필수입니다.")
        Integer milestoneNumber,

        List<String> labels,

        List<String> assignees
) {}
