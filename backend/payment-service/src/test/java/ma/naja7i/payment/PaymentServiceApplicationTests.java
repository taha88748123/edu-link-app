package ma.naja7i.payment;

import ma.naja7i.shared.events.DomainEvent;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Smoke test: full Spring context starts against a real Postgres container,
 * Flyway runs, JPA mappings validate. KafkaTemplate is mocked so the test is
 * hermetic. Skipped automatically when Docker is not available.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class PaymentServiceApplicationTests {

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("payment_db")
            .withUsername("payment_user")
            .withPassword("payment_pwd");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.cloud.config.enabled", () -> "false");
    }

    @TestConfiguration
    static class TestKafkaConfig {
        @Bean
        @Primary
        @SuppressWarnings({"unchecked", "rawtypes"})
        public KafkaTemplate<String, DomainEvent<?>> kafkaTemplate() {
            return (KafkaTemplate) org.mockito.Mockito.mock(KafkaTemplate.class);
        }
    }

    @Test
    void contextLoads() {
        // Spring context starts, Flyway runs all migrations on a clean DB.
    }
}
