package ma.naja7i.auth;

import ma.naja7i.shared.errors.GlobalExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@Import(GlobalExceptionHandler.class)
@EnableAsync
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
