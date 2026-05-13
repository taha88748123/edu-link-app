package ma.naja7i.pricing.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI pricingServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("NAJA7I — Pricing Service API")
                        .description("""
                                Pricing service: prof rates &amp; session packs, market suggestions, and
                                booking-time quote computation (rate + platform fee).

                                Endpoints under `/api/v1/pricing/**` (public + authenticated PROF flows)
                                and `/api/v1/internal/pricing/**` (server-to-server, protected by
                                `X-Internal-Key`).

                                Money is stored as `BIGINT` centimes (minor units) and exposed both as
                                `amountMinor` (raw) and `amountMad` (decimal string with 2 decimals) at the API edge.
                                """)
                        .version("v1")
                        .license(new License().name("Internal — NAJA7I")));
    }
}
