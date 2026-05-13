package ma.naja7i.booking.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.naja7i.booking.infrastructure.rest.CalendarInternalClient;
import ma.naja7i.booking.infrastructure.rest.CalendarSlotView;
import ma.naja7i.booking.infrastructure.rest.PricingInternalClient;
import ma.naja7i.booking.infrastructure.rest.QuoteResult;
import ma.naja7i.booking.infrastructure.web.dto.CreateBookingRequest;
import ma.naja7i.shared.dto.Modality;
import ma.naja7i.shared.dto.SchoolLevel;
import ma.naja7i.shared.events.DomainEvent;
import ma.naja7i.shared.security.JwtClaims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
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

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Smoke MVC test that:
 * <ol>
 *   <li>Boots context against a real Postgres container, runs Flyway.</li>
 *   <li>Mocks REST clients (calendar / pricing) + KafkaTemplate.</li>
 *   <li>STUDENT creates a booking and we assert the persisted view.</li>
 * </ol>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class BookingControllerIT {

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("booking_db")
            .withUsername("booking_user")
            .withPassword("booking_pwd");

    @DynamicPropertySource
    static void overrides(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
    }

    @MockBean CalendarInternalClient calendarInternalClient;
    @MockBean PricingInternalClient  pricingInternalClient;

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
    void create_endToEnd() throws Exception {
        UUID studentId = UUID.randomUUID();
        UUID profId    = UUID.randomUUID();
        UUID slotId    = UUID.randomUUID();
        Instant start  = Instant.now().plusSeconds(3600);

        when(pricingInternalClient.quote(eq(profId), eq("MATH"), eq(SchoolLevel.LYCEE),
                eq(Modality.ONLINE), eq(60), any(), any()))
                .thenReturn(new QuoteResult(15000L, 1500L, 16500L, "MAD"));
        when(calendarInternalClient.reserveSlot(eq(slotId), any(UUID.class)))
                .thenReturn(new CalendarSlotView(slotId, profId, start, start.plusSeconds(3600),
                        Modality.ONLINE, "RESERVED_PENDING", UUID.randomUUID(),
                        Instant.now().plusSeconds(600)));

        CreateBookingRequest req = new CreateBookingRequest(
                profId, slotId, "MATH", SchoolLevel.LYCEE, Modality.ONLINE, 60, null);

        mvc.perform(post("/api/v1/bookings")
                        .header(JwtClaims.HEADER_USER_ID, studentId.toString())
                        .header(JwtClaims.HEADER_USER_ROLE, "STUDENT")
                        .header(JwtClaims.HEADER_LOCALE, "fr")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.studentId").value(studentId.toString()))
                .andExpect(jsonPath("$.data.profId").value(profId.toString()))
                .andExpect(jsonPath("$.data.status").value("DEMANDEE"))
                .andExpect(jsonPath("$.data.amountMinor").value(15000))
                .andExpect(jsonPath("$.data.platformFeeMinor").value(1500))
                .andExpect(jsonPath("$.data.totalAmountMinor").value(16500))
                .andExpect(jsonPath("$.data.currency").value("MAD"));
    }
}
