package ma.naja7i.profile;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.minio.MinioClient;
import ma.naja7i.profile.application.FileStorageService;

/**
 * Boots the full Spring context against a real Postgres container to ensure wiring,
 * Flyway migrations, and JPA mappings are all consistent.
 *
 * <p>MinIO and Kafka clients are mocked so the test does not require external services.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class ProfileServiceApplicationTests {

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("profile_db")
            .withUsername("profile_user")
            .withPassword("profile_pwd");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @MockBean
    MinioClient minioClient;

    @MockBean
    FileStorageService fileStorageService;

    @TestConfiguration
    static class TestKafkaConfig {
        @Bean
        public org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate() {
            return org.mockito.Mockito.mock(org.springframework.kafka.core.KafkaTemplate.class);
        }
    }

    @Test
    void contextLoads() {
        // Verifies Spring context starts and Flyway migration succeeds.
    }
}
