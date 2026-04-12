package markoala.fithub.demo.auth;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    // 로그인 페이지
    @GetMapping("/login")
    public String loginPage() {
        return "login"; // login.html 템플릿 반환
    }

}
