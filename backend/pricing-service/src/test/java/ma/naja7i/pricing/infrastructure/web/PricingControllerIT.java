package ma.naja7i.pricing.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.naja7i.pricing.infrastructure.web.dto.CreateRateRequest;
import ma.naja7i.pricing.infrastructure.web.dto.QuoteRequest;
import ma.naja7i.shared.dto.Modality;
import ma.naja7i.shared.dto.SchoolLevel;
import ma.naja7i.shared.security.JwtClaims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Smoke MVC test against a real Postgres container:
 * <ol>
 *   <li>Boot context, run Flyway (V1 + V2 seed).</li>
 *   <li>PROF creates a rate via {@code POST /api/v1/pricing/rates}.</li>
 *   <li>{@code GET /api/v1/pricing/me} returns it.</li>
 *   <li>{@code POST /api/v1/pricing/quote} returns base + 10% platform fee.</li>
 *   <li>{@code POST /api/v1/internal/pricing/quote} without the internal key is rejected.</li>
 * </ol>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class PricingControllerIT {

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("pricing_db")
            .withUsername("pricing_user")
            .withPassword("pricing_pwd");

    @DynamicPropertySource
    static void overrides(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void createRate_then_quote_endToEnd() throws Exception {
        UUID profId = UUID.randomUUID();

        // 1. PROF creates a rate at 150.00 MAD (no dayOfWeek = applies to all days)
        CreateRateRequest createRate = new CreateRateRequest(
                "MATH", SchoolLevel.LYCEE, Modality.ONLINE, 60, null, "150.00");
        mvc.perform(post("/api/v1/pricing/rates")
                        .header(JwtClaims.HEADER_USER_ID, profId.toString())
                        .header(JwtClaims.HEADER_USER_ROLE, "PROF")
                        .header(JwtClaims.HEADER_LOCALE, "fr")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(createRate)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.amountMinor").value(15000))
                .andExpect(jsonPath("$.data.amountMad").value("150.00"))
                .andExpect(jsonPath("$.data.profId").value(profId.toString()));

        // 2. /me lists it
        mvc.perform(get("/api/v1/pricing/me")
                        .header(JwtClaims.HEADER_USER_ID, profId.toString())
                        .header(JwtClaims.HEADER_USER_ROLE, "PROF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rates[0].amountMad").value("150.00"))
                .andExpect(jsonPath("$.data.packs").isArray());

        // 3. quote: base 150.00 + 10% fee = 165.00
        QuoteRequest q = new QuoteRequest(
                profId, "MATH", SchoolLevel.LYCEE, Modality.ONLINE, 60, null, null);
        mvc.perform(post("/api/v1/pricing/quote")
                        .header(JwtClaims.HEADER_USER_ID, UUID.randomUUID().toString())
                        .header(JwtClaims.HEADER_USER_ROLE, "STUDENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(q)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.baseAmountMad").value("150.00"))
                .andExpect(jsonPath("$.data.platformFeeMad").value("15.00"))
                .andExpect(jsonPath("$.data.totalAmountMad").value("165.00"))
                .andExpect(jsonPath("$.data.breakdown.source").value("RATE"));

        // 4. internal endpoint without X-Internal-Key -> 401/403
        mvc.perform(post("/api/v1/internal/pricing/quote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(q)))
                .andExpect(status().is4xxClientError());

        // 5. internal endpoint with correct X-Internal-Key -> 200
        mvc.perform(post("/api/v1/internal/pricing/quote")
                        .header("X-Internal-Key", "test-internal-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(q)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAmountMad").value("165.00"));
    }
}
