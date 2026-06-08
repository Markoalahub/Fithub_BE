package markoala.fithub.demo.domain.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * GitHub API (/user/repos) 응답을 담기 위한 DTO
 */
public record GithubRepositoryDto(
        Long id,
        String name,
        @JsonProperty("full_name") String fullName,
        @JsonProperty("html_url") String htmlUrl,
        String description,
        @JsonProperty("private") boolean isPrivate,
        @JsonProperty("stargazers_count") int stargazersCount,
        @JsonProperty("open_issues_count") int openIssuesCount,
        String language,
        @JsonProperty("created_at") String createdAt,
        @JsonProperty("updated_at") String updatedAt,
        @JsonProperty("default_branch") String defaultBranch,
        @JsonProperty("pushed_at") String pushedAt,
        @JsonProperty("clone_url") String cloneUrl
) {}
