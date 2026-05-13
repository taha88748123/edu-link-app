package ma.naja7i.booking.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bookingServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("NAJA7I — Booking Service API")
                        .description("""
                                Booking lifecycle:
                                DEMANDEE -> ACCEPTEE_PAR_PROF -> EN_COURS -> TERMINEE -> CONFIRMEE
                                                                            \\           ANNULEE / LITIGE

                                Endpoints under `/api/v1/bookings/**` (STUDENT/PROF/ADMIN) and disputes
                                resolution under `/admin`. Anti-bypass: phone/email are never exposed by
                                this service.
                                """)
                        .version("v1")
                        .license(new License().name("Internal — NAJA7I")));
    }
}
