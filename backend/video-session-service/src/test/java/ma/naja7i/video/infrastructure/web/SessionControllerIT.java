package ma.naja7i.video.infrastructure.web;

import ma.naja7i.shared.events.DomainEvent;
import ma.naja7i.shared.security.JwtClaims;
import ma.naja7i.video.domain.Session;
import ma.naja7i.video.domain.SessionStatus;
import ma.naja7i.video.infrastructure.persistence.SessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Smoke IT: boots the full MVC stack against a real Postgres container, seeds
 * a session row through the repository and verifies the GET /me endpoint
 * returns it through the gateway-header auth path.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class SessionControllerIT {

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("video_db")
            .withUsername("video_user")
            .withPassword("video_pwd");

    @DynamicPropertySource
    static void overrides(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
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

    @Autowired MockMvc mvc;
    @Autowired SessionRepository sessionRepository;

    @Test
    void student_listsTheirSessions() throws Exception {
        UUID studentId = UUID.randomUUID();
        UUID profId    = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        Instant start  = Instant.now().plus(Duration.ofMinutes(30));

        Session seeded = Session.builder()
                .id(UUID.randomUUID())
                .bookingId(bookingId)
                .profId(profId)
                .studentId(studentId)
                .livekitRoom("naja7i-it-" + UUID.randomUUID().toString().substring(0, 8))
                .scheduledStart(start)
                .scheduledEnd(start.plus(Duration.ofMinutes(60)))
                .status(SessionStatus.SCHEDULED)
                .recordingEnabled(Boolean.FALSE)
                .version(0)
                .build();
        sessionRepository.saveAndFlush(seeded);

        mvc.perform(get("/api/v1/video/sessions/me")
                        .header(JwtClaims.HEADER_USER_ID, studentId.toString())
                        .header(JwtClaims.HEADER_USER_ROLE, "STUDENT")
                        .header(JwtClaims.HEADER_LOCALE, "fr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(seeded.getId().toString()))
                .andExpect(jsonPath("$.data[0].status").value("SCHEDULED"))
                .andExpect(jsonPath("$.data[0].studentId").value(studentId.toString()));
    }
}
