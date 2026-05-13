package ma.naja7i.profile.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI profileServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("NAJA7I — User Profile Service API")
                        .description("Manages profiles, prof onboarding, diplomas, reviews.")
                        .version("v1")
                        .license(new License().name("Proprietary")))
                .servers(List.of(new Server().url("/").description("Local")));
    }
}
