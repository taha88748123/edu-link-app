package ma.naja7i.ai.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI aiAgentServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("NAJA7I — AI Agent Service API")
                        .description("""
                                AI agents hub.

                                Operational in v1:
                                - `POST /api/v1/ai/matching/profs` (STUDENT) — suggests profs based on subject/level/modality.

                                Scoped for v2 (returns 501 NOT_IMPLEMENTED):
                                - `/api/v1/ai/qcm/generate`
                                - `/api/v1/ai/moderation/analyse`
                                - `/api/v1/ai/smart-reply/suggest`
                                - `/api/v1/ai/revision/generate`
                                - `/api/v1/ai/tutor/chat`

                                Driver fallback: `naja7i.ai.matching.driver` defaults to `logging`,
                                producing deterministic rationales without any Anthropic credentials.
                                Switch to `anthropic` to enable LLM-refined re-ranking.
                                """)
                        .version("v1")
                        .license(new License().name("Internal — NAJA7I")));
    }
}
