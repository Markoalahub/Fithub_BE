package markoala.fithub.demo.auth;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Hidden
public class LoginController {

    // 로그인 페이지
    @GetMapping("/login")
    public String loginPage() {
        return "login"; // login.html 템플릿 반환
    }

}
