package markoala.fithub.demo.github.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * 마일스톤 생성 요청 DTO
 *
 * @param title       마일스톤 제목
 * @param description 마일스톤 설명 (선택)
 * @param dueDate     마감일 (LocalDate → ISO 8601 변환)
 */
public record MilestoneRequest(
        @NotBlank(message = "마일스톤 제목은 필수입니다.")
        String title,

        String description,

        @NotNull(message = "마감일은 필수입니다.")
        LocalDate dueDate
) {}
