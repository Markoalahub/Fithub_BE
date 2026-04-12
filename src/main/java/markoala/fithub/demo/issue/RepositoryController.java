package markoala.fithub.demo.issue;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import markoala.fithub.demo.issue.dto.GithubRepositoryCreateRequest;
import markoala.fithub.demo.issue.dto.GithubRepositoryResponse;
import markoala.fithub.demo.project.ProjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/repositories")
@Tag(name = "Repositories", description = "프로젝트에 연결된 GitHub 레포지토리 관리 API")
public class RepositoryController {

    private final RepositoryRepository repositoryRepository;
    private final ProjectRepository projectRepository;

    public RepositoryController(RepositoryRepository repositoryRepository, ProjectRepository projectRepository) {
        this.repositoryRepository = repositoryRepository;
        this.projectRepository = projectRepository;
    }

    @GetMapping
    @Operation(summary = "프로젝트 레포 목록 조회",
            description = "프로젝트에 연결된 모든 GitHub 레포지토리를 조회합니다. 파이프라인 생성 시 직군(category) 확인에 사용됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "프로젝트를 찾을 수 없음")
    })
    public ResponseEntity<List<GithubRepositoryResponse>> getRepositories(
            @Parameter(description = "프로젝트 ID", required = true)
            @PathVariable Long projectId
    ) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        List<GithubRepositoryResponse> result = repositoryRepository.findByProjectId(projectId)
                .stream()
                .map(GithubRepositoryResponse::from)
                .toList();

        return ResponseEntity.ok(result);
    }

    @PostMapping
    @Operation(summary = "프로젝트에 레포 등록",
            description = "프로젝트에 GitHub 레포지토리를 연결합니다. category는 직군 구분에 사용됩니다 (예: FE, BE, AI).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "레포 등록 성공",
                    content = @Content(schema = @Schema(implementation = GithubRepositoryResponse.class))),
            @ApiResponse(responseCode = "404", description = "프로젝트를 찾을 수 없음")
    })
    public ResponseEntity<GithubRepositoryResponse> addRepository(
            @Parameter(description = "프로젝트 ID", required = true)
            @PathVariable Long projectId,
            @Valid @RequestBody GithubRepositoryCreateRequest request
    ) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        GithubRepository repo = GithubRepository.createRepository(
                projectId, request.repoUrl(), request.repoType(), request.category()
        );
        GithubRepository saved = repositoryRepository.save(repo);

        return ResponseEntity.status(HttpStatus.CREATED).body(GithubRepositoryResponse.from(saved));
    }

    @GetMapping("/{repositoryId}")
    @Operation(summary = "특정 레포 조회", description = "레포지토리 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "레포지토리를 찾을 수 없음")
    })
    public ResponseEntity<GithubRepositoryResponse> getRepository(
            @Parameter(description = "프로젝트 ID", required = true)
            @PathVariable Long projectId,
            @Parameter(description = "레포지토리 ID", required = true)
            @PathVariable Long repositoryId
    ) {
        GithubRepository repo = repositoryRepository.findById(repositoryId)
                .filter(r -> r.getProjectId().equals(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Repository not found: " + repositoryId));

        return ResponseEntity.ok(GithubRepositoryResponse.from(repo));
    }

    @DeleteMapping("/{repositoryId}")
    @Operation(summary = "레포 연결 해제", description = "프로젝트에서 레포지토리 연결을 해제합니다.")
    @ApiResponse(responseCode = "204", description = "삭제 성공")
    public ResponseEntity<Void> removeRepository(
            @Parameter(description = "프로젝트 ID", required = true)
            @PathVariable Long projectId,
            @Parameter(description = "레포지토리 ID", required = true)
            @PathVariable Long repositoryId
    ) {
        repositoryRepository.findById(repositoryId)
                .filter(r -> r.getProjectId().equals(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Repository not found: " + repositoryId));

        repositoryRepository.deleteById(repositoryId);
        return ResponseEntity.noContent().build();
    }
}
