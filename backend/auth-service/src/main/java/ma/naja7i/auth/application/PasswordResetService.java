package ma.naja7i.auth.application;

import lombok.RequiredArgsConstructor;
import ma.naja7i.auth.domain.AuditAction;
import ma.naja7i.auth.domain.User;
import ma.naja7i.auth.events.payload.PasswordResetRequestedPayload;
import ma.naja7i.auth.infrastructure.messaging.KafkaEventPublisher;
import ma.naja7i.auth.infrastructure.persistence.UserRepository;
import ma.naja7i.auth.infrastructure.redis.PasswordResetTokenStore;
import ma.naja7i.shared.errors.BusinessException;
import ma.naja7i.shared.errors.ErrorCode;
import ma.naja7i.shared.events.EventTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final SecureRandom RNG = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetTokenStore tokenStore;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AuthService authService;
    private final KafkaEventPublisher eventPublisher;

    /**
     * Issues a one-shot password-reset token. Always returns silently — never reveals
     * whether the account exists (defense against user enumeration).
     */
    @Transactional
    public void requestReset(String phoneOrEmail, String ip, String userAgent) {
        Optional<User> maybeUser = findByPhoneOrEmail(phoneOrEmail);
        if (maybeUser.isEmpty()) {
            log.info("Password reset requested for unknown account — ignored silently");
            return;
        }
        User user = maybeUser.get();
        String token = generateToken();
        tokenStore.store(token, user.getId());
        Instant expiresAt = Instant.now().plus(tokenStore.ttl());

        eventPublisher.publish(
                EventTopics.AUTH_PASSWORD_RESET,
                user.getId().toString(),
                "naja7i.auth.password.reset_requested.v1",
                new PasswordResetRequestedPayload(user.getId(), token, expiresAt));

        authService.audit(user.getId(), AuditAction.PASSWORD_RESET_REQUESTED, ip, userAgent);
    }

    @Transactional
    public void resetPassword(String token, String newPassword, String ip, String userAgent) {
        UUID userId = tokenStore.consume(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_TOKEN_INVALID,
                        "Invalid or expired reset token"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        tokenService.revokeAllForUser(userId);
        authService.audit(userId, AuditAction.PASSWORD_RESET, ip, userAgent);
    }

    Optional<User> findByPhoneOrEmail(String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        if (value.contains("@")) {
            return userRepository.findByEmail(value);
        }
        return userRepository.findByPhone(value);
    }

    String generateToken() {
        byte[] bytes = new byte[32];
        RNG.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
