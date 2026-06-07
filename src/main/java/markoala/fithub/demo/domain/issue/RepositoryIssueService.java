package markoala.fithub.demo.domain.issue;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import markoala.fithub.demo.domain.auth.GithubWebClientService;
import markoala.fithub.demo.domain.github.dto.GithubRepositoryDto;
import markoala.fithub.demo.domain.issue.dto.RepositoryIssueCreateRequest;
import markoala.fithub.demo.domain.issue.dto.RepositoryIssueCreateResponse;
import markoala.fithub.demo.domain.pipeline.client.PipelineV3Client;
import markoala.fithub.demo.domain.pipeline.dto.response.PipelineV3Response;
import markoala.fithub.demo.domain.user.User;
import markoala.fithub.demo.domain.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class RepositoryIssueService {

    private static final Logger log = LoggerFactory.getLogger(RepositoryIssueService.class);

    private final UserService userService;
    private final GithubWebClientService githubWebClientService;
    private final PipelineV3Client pipelineV3Client;

    public RepositoryIssueCreateResponse createIssue(Long userId, Long pipelineId, RepositoryIssueCreateRequest request) {
        log.info("[Repository Issue] Creating GitHub issue requested. userId={}, pipelineId={}, featDetail={}",
                userId, pipelineId, request.featDetail());

        String githubAccessToken = getGithubAccessToken(userId);
        WebClient webClient = githubWebClientService.getWebClient(githubAccessToken);
        PipelineV3Response pipeline = getPipeline(pipelineId);
        GithubRepositoryDto repository = getRepositoryByPipeline(webClient, pipeline);

        String title = request.featDetail().trim();
        String body = request.body();
        String[] repoPath = splitRepositoryPath(repository.fullName());

        GithubIssueCreateApiResponse createdIssue = webClient.post()
                .uri("/repos/{owner}/{repo}/issues", repoPath[0], repoPath[1])
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new GithubIssueCreateApiRequest(title, body))
                .retrieve()
                .bodyToMono(GithubIssueCreateApiResponse.class)
                .block();

        if (createdIssue == null) {
            log.warn("[Repository Issue] Empty GitHub issue creation response. userId={}, repoId={}, pipelineId={}, repoFullName={}",
                    userId, repository.id(), pipeline.id(), repository.fullName());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "GitHub 이슈 생성 응답이 비어있습니다.");
        }

        log.info("[Repository Issue] GitHub issue created. userId={}, repoId={}, pipelineId={}, repoFullName={}, issueNumber={}, issueUrl={}",
                userId, repository.id(), pipeline.id(), repository.fullName(), createdIssue.number(), createdIssue.htmlUrl());

        return new RepositoryIssueCreateResponse(
                repository.id(),
                pipeline.id(),
                createdIssue.number(),
                createdIssue.htmlUrl(),
                createdIssue.title(),
                createdIssue.body(),
                createdIssue.state()
        );
    }

    private String getGithubAccessToken(Long userId) {
        if (userId == null) {
            log.warn("[Repository Issue] Missing authenticated userId.");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }

        User user = userService.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다: " + userId));

        String githubAccessToken = user.getGithubAccessToken();
        if (githubAccessToken == null || githubAccessToken.isBlank()) {
            log.warn("[Repository Issue] Missing GitHub access token. userId={}", userId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "GitHub access token이 저장되지 않았습니다. GitHub 로그인 후 다시 시도해주세요.");
        }

        return githubAccessToken;
    }

    private GithubRepositoryDto getRepositoryByPipeline(WebClient webClient, PipelineV3Response pipeline) {
        String pipelineRepoUrl = pipeline.githubRepoUrl();
        if (pipelineRepoUrl == null || pipelineRepoUrl.isBlank()) {
            log.warn("[Repository Issue] Pipeline has no GitHub repository URL. pipelineId={}", pipeline.id());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "파이프라인에 연결된 GitHub repository URL이 없습니다. 먼저 레포지토리를 연결해주세요.");
        }

        String[] repoPath = extractRepoPath(pipelineRepoUrl);

        try {
            log.info("[Repository Issue] Fetching GitHub repository by pipeline URL. pipelineId={}, pipelineRepoUrl={}, owner={}, repo={}",
                    pipeline.id(), pipelineRepoUrl, repoPath[0], repoPath[1]);

            GithubRepositoryDto repository = webClient.get()
                    .uri("/repos/{owner}/{repo}", repoPath[0], repoPath[1])
                    .retrieve()
                    .bodyToMono(GithubRepositoryDto.class)
                    .block();

            if (repository == null) {
                log.warn("[Repository Issue] Empty GitHub repository response. pipelineId={}, pipelineRepoUrl={}",
                        pipeline.id(), pipelineRepoUrl);
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "파이프라인에 연결된 GitHub repository URL의 레포지토리를 찾을 수 없습니다.");
            }

            log.info("[Repository Issue] GitHub repository fetched. pipelineId={}, repoId={}, repoFullName={}, repoHtmlUrl={}, pipelineRepoUrl={}, normalizedPipelineRepoUrl={}, normalizedGithubRepoUrl={}",
                    pipeline.id(),
                    repository.id(),
                    repository.fullName(),
                    repository.htmlUrl(),
                    pipelineRepoUrl,
                    normalizeGithubRepoUrl(pipelineRepoUrl),
                    normalizeGithubRepoUrl(repository.htmlUrl()));

            return repository;
        } catch (WebClientResponseException.NotFound e) {
            log.warn("[Repository Issue] GitHub repository not found. pipelineId={}, pipelineRepoUrl={}, owner={}, repo={}",
                    pipeline.id(), pipelineRepoUrl, repoPath[0], repoPath[1]);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "파이프라인에 연결된 GitHub repository URL의 레포지토리를 찾을 수 없습니다.", e);
        }
    }

    private PipelineV3Response getPipeline(Long pipelineId) {
        log.info("[Repository Issue] Fetching pipeline for repository validation. pipelineId={}", pipelineId);

        try {
            PipelineV3Response pipeline = pipelineV3Client.getPipeline(pipelineId);
            if (pipeline == null) {
                log.warn("[Repository Issue] Empty pipeline response. pipelineId={}", pipelineId);
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "파이프라인을 찾을 수 없습니다.");
            }

            log.info("[Repository Issue] Pipeline fetched. pipelineId={}, projectId={}, category={}, pipelineGithubRepoUrl={}",
                    pipeline.id(), pipeline.projectId(), pipeline.category(), pipeline.githubRepoUrl());

            return pipeline;
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
                log.warn("[Repository Issue] Pipeline not found from Python server. pipelineId={}, status={}, responseBody={}",
                        pipelineId, e.getStatusCode().value(), e.getResponseBodyAsString());
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "파이프라인을 찾을 수 없습니다.", e);
            }
            throw e;
        }
    }

    private String normalizeGithubRepoUrl(String repoUrl) {
        if (repoUrl == null) {
            return "";
        }

        String normalized = repoUrl.trim();
        if (normalized.endsWith(".git")) {
            normalized = normalized.substring(0, normalized.length() - 4);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String[] extractRepoPath(String repoUrl) {
        String normalized = normalizeGithubRepoUrl(repoUrl);
        normalized = normalized.replaceFirst("^https://github\\.com/", "");
        normalized = normalized.replaceFirst("^http://github\\.com/", "");
        normalized = normalized.replaceFirst("^git@github\\.com:", "");

        String[] parts = normalized.split("/");
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "파이프라인에 연결된 GitHub repository URL 형식이 올바르지 않습니다.");
        }

        return parts;
    }

    private String[] splitRepositoryPath(String fullName) {
        if (fullName == null || !fullName.contains("/")) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "GitHub 저장소 이름 형식이 올바르지 않습니다.");
        }

        String[] parts = fullName.split("/", 2);
        if (parts[0].isBlank() || parts[1].isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "GitHub 저장소 이름 형식이 올바르지 않습니다.");
        }

        return parts;
    }

    private record GithubIssueCreateApiRequest(
            String title,
            String body
    ) {}

    private record GithubIssueCreateApiResponse(
            Integer number,
            String title,
            String body,
            String state,
            @JsonProperty("html_url") String htmlUrl
    ) {}
}
