package ma.naja7i.calendar.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI calendarServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("NAJA7I — Calendar Service API")
                        .description("""
                                Availability rules + slot materialization + slot lifecycle (AVAILABLE
                                → RESERVED_PENDING → BOOKED / BLOCKED, with TTL-driven release).

                                Endpoints under `/api/v1/calendar/**` (public/PROF) and
                                `/api/v1/internal/calendar/**` (booking-service via shared key).
                                """)
                        .version("v1")
                        .license(new License().name("Internal — NAJA7I")));
    }
}
