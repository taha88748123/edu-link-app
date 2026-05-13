package ma.naja7i.auth.infrastructure.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ma.naja7i.auth.events.payload.OtpRequestedPayload;
import ma.naja7i.auth.infrastructure.messaging.KafkaEventPublisher;
import ma.naja7i.auth.infrastructure.web.dto.LoginRequest;
import ma.naja7i.auth.infrastructure.web.dto.RegisterRequest;
import ma.naja7i.auth.infrastructure.web.dto.VerifyOtpRequest;
import ma.naja7i.shared.dto.Role;
import ma.naja7i.shared.events.EventTopics;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class AuthControllerIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("auth_db")
            .withUsername("test")
            .withPassword("test");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void overrides(DynamicPropertyRegistry r) {
        POSTGRES.start();
        REDIS.start();
        r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        r.add("spring.datasource.username", POSTGRES::getUsername);
        r.add("spring.datasource.password", POSTGRES::getPassword);
        r.add("spring.data.redis.host", REDIS::getHost);
        r.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        // disable infra we don't run inside the test
        r.add("spring.cloud.config.enabled", () -> "false");
        r.add("spring.cloud.discovery.enabled", () -> "false");
        r.add("eureka.client.enabled", () -> "false");
        r.add("spring.kafka.bootstrap-servers", () -> "PLAINTEXT://localhost:9999");
        r.add("naja7i.jwt.secret",
                () -> "test-jwt-secret-must-be-at-least-32-chars-long-aaa");
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean KafkaEventPublisher publisher;

    @Test
    void registerVerifyLogin_endToEnd() throws Exception {
        // ----- 1. register
        RegisterRequest reg = new RegisterRequest(
                "+212600000099", "it-student@example.com",
                "strong-secret-pw", Role.STUDENT, "fr");

        String regJson = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING_OTP"))
                .andReturn().getResponse().getContentAsString();

        JsonNode regBody = objectMapper.readTree(regJson);
        UUID userId = UUID.fromString(regBody.get("data").get("userId").asText());

        // Capture the OTP that was "published" via KafkaEventPublisher
        ArgumentCaptor<OtpRequestedPayload> otpCaptor =
                ArgumentCaptor.forClass(OtpRequestedPayload.class);
        verify(publisher, atLeastOnce()).publish(
                eq(EventTopics.AUTH_OTP_REQUESTED),
                eq(userId.toString()),
                any(),
                otpCaptor.capture());
        String code = otpCaptor.getValue().code();
        assertThat(code).matches("\\d{6}");

        // ----- 2. verify OTP
        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new VerifyOtpRequest(userId, code))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());

        // ----- 3. login (now ACTIVE)
        String loginJson = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("+212600000099", "strong-secret-pw"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        assertThat(loginJson).contains("accessToken");

        // ----- 4. /me with header-based authentication
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(userId.toString()))
                .andExpect(jsonPath("$.data.role").value("STUDENT"));
    }
}
