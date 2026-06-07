package markoala.fithub.demo.domain.issue.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 사용자의 GitHub 저장소 목록 응답
 * 사용자가 자신의 GitHub 계정에서 소유한 저장소들을 조회합니다
 */
@Schema(description = "현재 사용자가 접근 가능한 GitHub 저장소 목록 응답")
public record GitHubUserRepositoriesResponse(
        @Schema(description = "GitHub 저장소 목록")
        List<AvailableGithubRepository> repositories,

        @Schema(description = "조회된 저장소 총 개수", example = "1")
        int totalCount
) {
    @Schema(description = "GitHub 저장소 요약 정보")
    public record AvailableGithubRepository(
            @JsonProperty("repo_id")
            @Schema(name = "repo_id", description = "GitHub 저장소 ID", example = "123456789")
            Long repoId,

            @JsonProperty("repo_name")
            @Schema(name = "repo_name", description = "저장소 이름", example = "Fithub_BE")
            String repoName,

            @JsonProperty("repo_url_name")
            @Schema(name = "repo_url_name", description = "소유자/저장소 형식의 전체 이름", example = "Markoalahub/Fithub_BE")
            String repoUrlName,

            @Schema(description = "저장소 설명", example = "Spring server")
            String description,

            @JsonProperty("repo_url")
            @Schema(name = "repo_url", description = "GitHub 웹 URL", example = "https://github.com/Markoalahub/Fithub_BE")
            String repoUrl,

            @Schema(description = "private 저장소 여부", example = "false")
            boolean isPrivate,

            @Schema(description = "대표 언어", example = "Java")
            String language,

            @Schema(name = "starCount", description = "스타 수", example = "3")
            int starCount,

            @Schema(description = "열린 이슈 수", example = "2")
            int openIssuesCount
    ) {}
}
