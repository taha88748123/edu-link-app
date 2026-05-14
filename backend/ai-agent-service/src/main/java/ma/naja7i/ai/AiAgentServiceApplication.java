package ma.naja7i.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * NAJA7I AI Agent Service entry point.
 *
 * <p>For MVP v1, only the MATCHING agent is operational. The other agents
 * (QCM / MODERATION / SMART_REPLY / REVISION / TUTOR) are wired as 501
 * stubs so the API surface stays stable.</p>
 *
 * <p>The default driver is {@code logging}: the service boots without
 * any Anthropic credentials and falls back to a deterministic baseline
 * scorer. Switch to {@code anthropic} via {@code naja7i.ai.matching.driver}
 * once a real API key is available.</p>
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableJpaRepositories
@EnableScheduling
@EnableKafka
public class AiAgentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiAgentServiceApplication.class, args);
    }
}
