package ma.naja7i.auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI authServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("NAJA7I — Auth Service API")
                        .description("""
                                Authentication, OTP verification, JWT issuance and refresh-token rotation.

                                Endpoints under `/api/v1/auth/**`. Public endpoints (register, login,
                                verify-otp, refresh, password reset) bypass JWT verification at the gateway.
                                """)
                        .version("v1")
                        .license(new License().name("Internal — NAJA7I")));
    }
}
