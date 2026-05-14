package ma.naja7i.video.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI videoSessionServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("NAJA7I — Video Session Service API")
                        .description("""
                                Control plane for WebRTC sessions via LiveKit.

                                Responsibilities:
                                - Issue short-lived LiveKit JWT tokens (no media flows through us).
                                - Track session lifecycle (SCHEDULED -> LIVE -> ENDED).
                                - Capture artifacts (whiteboard / quiz / note / code).
                                - Manage participant recording consent.

                                Endpoints:
                                - STUDENT / PROF: token, :start, :end, sessions, consent, artifacts
                                - ADMIN:          list / :expire
                                """)
                        .version("v1")
                        .license(new License().name("Internal — NAJA7I")));
    }
}
