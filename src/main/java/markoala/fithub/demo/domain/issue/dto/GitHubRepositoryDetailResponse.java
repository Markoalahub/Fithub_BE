package markoala.fithub.demo.domain.issue.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "현재 사용자가 접근 가능한 GitHub 저장소 단일 조회 응답")
public record GitHubRepositoryDetailResponse(
        @JsonProperty("repo_id")
        @Schema(name = "repo_id", description = "GitHub 저장소 ID", example = "1028238282")
        Long repoId,

        @JsonProperty("repo_name")
        @Schema(name = "repo_name", description = "저장소 이름", example = "Fin_AI_Agent")
        String repoName,

        @JsonProperty("repo_url_name")
        @Schema(name = "repo_url_name", description = "소유자/저장소 형식의 전체 이름", example = "tomchaccom/Fin_AI_Agent")
        String repoUrlName,

        @Schema(description = "저장소 설명", example = "미래에셋 AI 공모전 AI agent")
        String description,

        @JsonProperty("repo_url")
        @Schema(name = "repo_url", description = "GitHub 웹 URL", example = "https://github.com/tomchaccom/Fin_AI_Agent")
        String repoUrl,

        @Schema(description = "private 저장소 여부", example = "false")
        boolean isPrivate,

        @Schema(description = "대표 언어", example = "Python")
        String language,

        @Schema(name = "starCount", description = "스타 수", example = "0")
        int starCount,

        @Schema(description = "열린 이슈 수", example = "0")
        int openIssuesCount,

        @Schema(description = "기본 브랜치", example = "main")
        String defaultBranch,

        @Schema(description = "저장소 수정 시각", example = "2026-06-05T00:00:00Z")
        String updatedAt,

        @Schema(description = "마지막 push 시각", example = "2026-06-05T00:00:00Z")
        String pushedAt,

        @Schema(description = "HTTPS clone URL", example = "https://github.com/tomchaccom/Fin_AI_Agent.git")
        String cloneUrl
) {}
