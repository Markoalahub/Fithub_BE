package markoala.fithub.demo.issue.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * GitHub 저장소를 프로젝트에 동기화하는 요청
 * 사용자가 선택한 저장소들을 프로젝트에 일괄 등록합니다
 */
public record SyncGithubRepositoriesRequest(
        @NotNull(message = "Repository IDs are required")
        @NotEmpty(message = "At least one repository must be selected")
        List<Long> githubRepoIds,

        @NotNull(message = "Category mapping is required")
        List<RepositoryCategoryMapping> categoryMappings
) {
    /**
     * GitHub 저장소 ID별 카테고리 매핑
     */
    public record RepositoryCategoryMapping(
            Long githubRepoId,
            String repoName,
            String category  // FE, BE, AI, DEVOPS, QA, etc.
    ) {}
}
