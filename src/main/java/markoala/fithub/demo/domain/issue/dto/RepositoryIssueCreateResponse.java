package markoala.fithub.demo.domain.issue.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "파이프라인 연결 레포지토리 GitHub 이슈 생성 응답")
public record RepositoryIssueCreateResponse(
        @JsonProperty("repo_id")
        @Schema(name = "repo_id", description = "GitHub 저장소 ID", example = "1028238282")
        Long repoId,

        @JsonProperty("pipeline_id")
        @Schema(name = "pipeline_id", description = "파이프라인 ID", example = "33")
        Long pipelineId,

        @JsonProperty("github_issue_number")
        @Schema(name = "github_issue_number", description = "GitHub 이슈 번호", example = "42")
        Integer githubIssueNumber,

        @JsonProperty("github_issue_url")
        @Schema(name = "github_issue_url", description = "GitHub 이슈 웹 URL", example = "https://github.com/tomchaccom/Fin_AI_Agent/issues/42")
        String githubIssueUrl,

        @Schema(description = "GitHub 이슈 제목", example = "로그인 API 구현")
        String title,

        @Schema(description = "GitHub 이슈 본문", example = "JWT 기반 로그인 API를 구현합니다.")
        String body,

        @Schema(description = "GitHub 이슈 상태", example = "open")
        String state
) {}
