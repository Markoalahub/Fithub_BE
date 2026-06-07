package markoala.fithub.demo.domain.issue.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "파이프라인 연결 레포지토리 GitHub 이슈 생성 요청")
public record RepositoryIssueCreateRequest(
        @JsonProperty("feat_detail")
        @NotBlank(message = "feat_detail은 필수입니다.")
        @Schema(name = "feat_detail", description = "GitHub 이슈 제목으로 사용할 작업 상세 내용", example = "로그인 API 구현")
        String featDetail,

        @Schema(description = "GitHub 이슈 본문. Markdown을 사용할 수 있습니다.", example = "JWT 기반 로그인 API를 구현합니다.")
        String body
) {}
