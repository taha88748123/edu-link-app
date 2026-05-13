package ma.naja7i.payment.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.naja7i.payment.domain.TopupMethod;
import ma.naja7i.payment.infrastructure.web.dto.CreateTopupRequest;
import ma.naja7i.shared.events.DomainEvent;
import ma.naja7i.shared.security.JwtClaims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boots the full MVC stack against a real Postgres container, exercises a
 * STUDENT top-up submission end-to-end and verifies the wallet view.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class PaymentControllerIT {

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("payment_db")
            .withUsername("payment_user")
            .withPassword("payment_pwd");

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
    @Autowired ObjectMapper objectMapper;

    @Test
    void student_submitsTopup_andQueriesBalance() throws Exception {
        UUID studentId = UUID.randomUUID();

        CreateTopupRequest req = new CreateTopupRequest(
                TopupMethod.CASHPLUS, new BigDecimal("150.00"), null);

        mvc.perform(post("/api/v1/payment/topups")
                        .header(JwtClaims.HEADER_USER_ID, studentId.toString())
                        .header(JwtClaims.HEADER_USER_ROLE, "STUDENT")
                        .header(JwtClaims.HEADER_LOCALE, "fr")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.studentId").value(studentId.toString()))
                .andExpect(jsonPath("$.data.amountMinor").value(15000))
                .andExpect(jsonPath("$.data.method").value("CASHPLUS"))
                .andExpect(jsonPath("$.data.status").value("EN_ATTENTE_ADMIN"));

        // Balance is still zero (no admin approval yet).
        mvc.perform(get("/api/v1/payment/balance/me")
                        .header(JwtClaims.HEADER_USER_ID, studentId.toString())
                        .header(JwtClaims.HEADER_USER_ROLE, "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balanceMinor").value(0))
                .andExpect(jsonPath("$.data.currency").value("MAD"));
    }
}
