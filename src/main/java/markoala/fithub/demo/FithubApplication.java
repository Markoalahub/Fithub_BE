package markoala.fithub.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class FithubApplication {

	public static void main(String[] args) {
		SpringApplication.run(FithubApplication.class, args);
	}

}
