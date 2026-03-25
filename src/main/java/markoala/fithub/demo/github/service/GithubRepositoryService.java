package markoala.fithub.demo.github.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import markoala.fithub.demo.auth.service.GithubWebClientService;
import markoala.fithub.demo.github.dto.GithubRepositoryDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GithubRepositoryService {

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
