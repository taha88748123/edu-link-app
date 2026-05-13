package ma.naja7i.notification.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI notificationServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("NAJA7I — Notification Service API")
                        .description("""
                                Multi-channel notification dispatcher (SMS / WhatsApp / Email / Push).

                                Primarily event-driven (Kafka consumer) — the REST surface is small :

                                - `POST /api/v1/notifications/send`  (internal only — X-Internal-Key or ADMIN role)
                                - `GET  /api/v1/notifications/me`     paginated history of the caller
                                - `GET  /api/v1/notifications/admin/log` filterable log for support

                                Channel adapters default to `logging` so the service boots without any
                                Twilio / SendGrid / FCM credentials.
                                """)
                        .version("v1")
                        .license(new License().name("Internal — NAJA7I")));
    }
}
