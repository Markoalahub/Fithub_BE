package markoala.fithub.demo.github.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import markoala.fithub.demo.auth.GithubWebClientService;
import markoala.fithub.demo.github.dto.GithubRepositoryDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GithubRepositoryService {

    private static final Logger log = LoggerFactory.getLogger(GithubRepositoryService.class);

    private final GithubWebClientService githubWebClientService;

    public List<GithubRepositoryDto> getMyRepos() {
        var auth = githubWebClientService.getAuthInfo();
        var webClient = githubWebClientService.getWebClient(auth.accessToken());

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/user/repos")
                        .queryParam("per_page", "100")
                        .queryParam("sort", "created")
                        .queryParam("direction", "desc")
                        .build())
                .retrieve()
                .bodyToFlux(GithubRepositoryDto.class)
                .collectList()
                .block();
    }
}
