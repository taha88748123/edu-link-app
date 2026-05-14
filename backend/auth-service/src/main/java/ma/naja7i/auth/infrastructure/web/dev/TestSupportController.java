package ma.naja7i.auth.infrastructure.web.dev;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import ma.naja7i.auth.config.OtpProperties;
import ma.naja7i.auth.infrastructure.redis.OtpStore;
import ma.naja7i.shared.dto.ApiResponse;
import ma.naja7i.shared.errors.BusinessException;
import ma.naja7i.shared.errors.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

/**
 * DEV-ONLY backdoor that lets the E2E test runner read the OTP code that
 * was just issued for a user (since SMS / WhatsApp delivery is mocked in dev).
 *
 * <h3>Security model — triple gate</h3>
 * <ol>
 *   <li><b>Spring property gate</b> — class is only created as a bean when
 *       {@code naja7i.auth.dev.test-mode=true}. The property defaults to
 *       {@code false} everywhere; CI sets the {@code NAJA7I_AUTH_TEST_MODE}
 *       env var explicitly only on the e2e-ci workflow. In production this
 *       endpoint <b>does not exist</b> — Spring never registers the
 *       handler.</li>
 *   <li><b>Header gate</b> — each method additionally requires the
 *       {@code X-Test-Mode: true} request header. Belt-and-suspenders so a
 *       misconfigured prod deployment still 403s.</li>
 *   <li><b>Public-path restriction</b> — the dev path
 *       {@code /api/v1/dev/auth/**} is intentionally NOT added to the
 *       gateway's PublicPaths list, so even if test mode leaks the endpoint
 *       is unreachable from the public Internet.</li>
 * </ol>
 *
 * Under no circumstances may this be enabled in production. See
 * {@code frontend-web/e2e/README.md} for usage details.
 */
@RestController
@RequestMapping("/api/v1/dev/auth")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "naja7i.auth.dev", name = "test-mode", havingValue = "true")
@Hidden
@Tag(name = "Dev test support", description = "DEV-ONLY endpoints for the e2e test harness")
public class TestSupportController {

    private static final Logger log = LoggerFactory.getLogger(TestSupportController.class);

    private final OtpStore otpStore;
    private final OtpProperties otpProperties;

    /** Returned shape: {@code { code, channel, expiresAt }}. */
    public record OtpView(String code, String channel, Instant expiresAt) {}

    /**
     * Reads the most recent OTP for the user from Redis. Requires the
     * {@code X-Test-Mode: true} header — sent without it, the request is
     * rejected with 403 even though the endpoint exists.
     */
    @GetMapping("/otp/{userId}")
    @Operation(summary = "DEV-ONLY: peek at the OTP code for a user")
    public ApiResponse<OtpView> peekOtp(
            @PathVariable UUID userId,
            @RequestHeader(value = "X-Test-Mode", required = false) String testHeader) {
        requireTestHeader(testHeader);

        String code = otpStore.findPlainCode(userId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "AUTH_OTP_NOT_FOUND: no active OTP for this user"));

        Instant expiresAt = Instant.now().plus(otpProps().getTtl());
        log.warn("DEV: OTP peek requested for user {} — test-mode endpoint", userId);
        return ApiResponse.of(new OtpView(code, "SMS", expiresAt));
    }

    private OtpProperties otpProps() { return otpProperties; }

    private static void requireTestHeader(String header) {
        if (header == null || !"true".equalsIgnoreCase(header.trim())) {
            // 403 because the request is well-formed but lacks the secondary auth.
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN_ROLE,
                    "X-Test-Mode header is required for dev endpoints");
        }
    }
}
