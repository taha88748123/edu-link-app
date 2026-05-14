package ma.naja7i.auth.infrastructure.web.dev;

import ma.naja7i.auth.config.OtpProperties;
import ma.naja7i.auth.infrastructure.redis.OtpStore;
import ma.naja7i.shared.errors.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit test for the DEV-ONLY OTP backdoor. We do not bootstrap the full Spring
 * context — the controller is wired manually through MockMvc with the standard
 * exception handler. The @ConditionalOnProperty gate is verified separately
 * in the integration test (only registered when test-mode=true).
 */
class TestSupportControllerTest {

    private OtpStore otpStore;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        otpStore = mock(OtpStore.class);
        OtpProperties props = new OtpProperties();
        props.setTtl(Duration.ofMinutes(5));
        TestSupportController controller = new TestSupportController(otpStore, props);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsOtpCode_whenHeaderPresentAndCodeExists() throws Exception {
        UUID userId = UUID.randomUUID();
        when(otpStore.findPlainCode(userId)).thenReturn(Optional.of("123456"));

        mockMvc.perform(get("/api/v1/dev/auth/otp/{id}", userId)
                        .header("X-Test-Mode", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("123456"))
                .andExpect(jsonPath("$.data.channel").value("SMS"))
                .andExpect(jsonPath("$.data.expiresAt").isNotEmpty());
    }

    @Test
    void rejects_whenHeaderMissing() throws Exception {
        UUID userId = UUID.randomUUID();
        // Even if the code exists, the request is refused without the header.
        when(otpStore.findPlainCode(any())).thenReturn(Optional.of("123456"));

        mockMvc.perform(get("/api/v1/dev/auth/otp/{id}", userId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("AUTH_FORBIDDEN_ROLE"));
    }

    @Test
    void rejects_whenHeaderValueIsNotTrue() throws Exception {
        UUID userId = UUID.randomUUID();
        when(otpStore.findPlainCode(any())).thenReturn(Optional.of("123456"));

        mockMvc.perform(get("/api/v1/dev/auth/otp/{id}", userId)
                        .header("X-Test-Mode", "false"))
                .andExpect(status().isForbidden());
    }

    @Test
    void returns404_whenNoActiveOtp() throws Exception {
        UUID userId = UUID.randomUUID();
        when(otpStore.findPlainCode(userId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/dev/auth/otp/{id}", userId)
                        .header("X-Test-Mode", "true"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }
}
