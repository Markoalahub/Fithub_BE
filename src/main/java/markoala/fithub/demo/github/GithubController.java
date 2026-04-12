package markoala.fithub.demo.github;

import lombok.RequiredArgsConstructor;
import markoala.fithub.demo.github.dto.GithubRepositoryDto;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class GithubController {

    private final GithubRepositoryService githubRepositoryService;

    @GetMapping("/home")
    public String home(Model model) {
        // 1. 서비스에서 레포지토리 목록을 가져옵니다.
        List<GithubRepositoryDto> repos = githubRepositoryService.getMyRepos();

        // 2. 뷰에서 사용할 이름("repos")으로 모델에 데이터를 담습니다.
        model.addAttribute("repos", repos);

        // 3. home.html 템플릿을 반환합니다.
        return "home";
    }
}
