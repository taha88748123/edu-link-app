package ma.naja7i.calendar;

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
 * Boots the full Spring context against a real Postgres container to verify wiring,
 * Flyway migrations and JPA mappings. The Kafka template is mocked so the test is
 * hermetic and does not require a Kafka broker.
 *
 * <p>Auto-skipped when Docker is not available on the host (CI conditional behaviour).
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class CalendarServiceApplicationTests {

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("calendar_db")
            .withUsername("calendar_user")
            .withPassword("calendar_pwd");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // Eureka + Config server are off for tests
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
