package ma.naja7i.auth.application;

import lombok.RequiredArgsConstructor;
import ma.naja7i.auth.config.OtpProperties;
import ma.naja7i.auth.domain.OtpChannel;
import ma.naja7i.auth.domain.User;
import ma.naja7i.auth.events.payload.OtpRequestedPayload;
import ma.naja7i.auth.infrastructure.messaging.KafkaEventPublisher;
import ma.naja7i.auth.infrastructure.redis.OtpStore;
import ma.naja7i.auth.infrastructure.redis.RateLimiter;
import ma.naja7i.shared.errors.BusinessException;
import ma.naja7i.shared.errors.ErrorCode;
import ma.naja7i.shared.events.EventTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private static final SecureRandom RNG = new SecureRandom();

    private final OtpStore otpStore;
    private final RateLimiter rateLimiter;
    private final OtpProperties otpProperties;
    private final KafkaEventPublisher eventPublisher;

    /**
     * Generates a fresh OTP, stores its hash in Redis and publishes {@code auth.otp.requested}.
     * Bypasses rate limiting (intended for the registration path).
     */
    public void issueAndPublish(User user, OtpChannel channel) {
        String code = generateCode();
        otpStore.store(user.getId(), code);
        Instant expiresAt = Instant.now().plus(otpProperties.getTtl());
        eventPublisher.publish(
                EventTopics.AUTH_OTP_REQUESTED,
                user.getId().toString(),
                "naja7i.auth.otp.requested.v1",
                new OtpRequestedPayload(
                        user.getId(),
                        user.getPhone(),
                        code,
                        channel,
                        expiresAt,
                        user.getLocale()
                ));
        log.info("OTP issued for user (channel={})", channel);
    }

    /** Rate-limited resend. Throws RATE_LIMITED if the bucket is full. */
    public void resend(User user, OtpChannel channel) {
        if (!rateLimiter.tryAcquireOtpResend(user.getPhone())) {
            throw new BusinessException(ErrorCode.RATE_LIMITED,
                    "Too many OTP requests, please wait before retrying");
        }
        issueAndPublish(user, channel);
    }

    /**
     * Validates the presented code. Increments the attempt counter and throws on mismatch /
     * expiry / max attempts.
     */
    public void verify(User user, String presentedCode) {
        if (otpStore.findHash(user.getId()).isEmpty()) {
            throw new BusinessException(ErrorCode.AUTH_OTP_EXPIRED, "OTP expired or not requested");
        }
        int attempts = otpStore.incrementAttempts(user.getId());
        if (attempts > otpProperties.getMaxAttempts()) {
            otpStore.clear(user.getId());
            throw new BusinessException(ErrorCode.AUTH_OTP_INVALID, "Maximum attempts exceeded");
        }
        if (!otpStore.verify(user.getId(), presentedCode)) {
            throw new BusinessException(ErrorCode.AUTH_OTP_INVALID, "Invalid OTP code");
        }
        otpStore.clear(user.getId());
    }

    String generateCode() {
        int length = otpProperties.getCodeLength();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(RNG.nextInt(10));
        }
        return sb.toString();
    }
}
