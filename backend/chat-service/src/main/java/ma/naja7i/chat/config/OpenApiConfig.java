package ma.naja7i.chat.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI chatServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("NAJA7I — Chat Service API")
                        .description("""
                                In-app chat between PROF and STUDENT around a booking.

                                Conversation lifecycle: PRE_ACCEPT -> POST_ACCEPT -> CLOSED.

                                Anti-bypass: in PRE_ACCEPT, phone / email / social URLs are
                                masked synchronously (regex). An optional Claude-driven async
                                analysis raises bypass-intent flags. Strike tracker on Redis
                                escalates to CRITICAL after a threshold.
                                """)
                        .version("v1")
                        .license(new License().name("Internal — NAJA7I")));
    }
}
