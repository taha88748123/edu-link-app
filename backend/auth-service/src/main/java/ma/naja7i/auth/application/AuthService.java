package ma.naja7i.auth.application;

import lombok.RequiredArgsConstructor;
import ma.naja7i.auth.domain.AuditAction;
import ma.naja7i.auth.domain.AuditLog;
import ma.naja7i.auth.domain.OtpChannel;
import ma.naja7i.auth.domain.User;
import ma.naja7i.auth.domain.UserStatus;
import ma.naja7i.auth.events.payload.UserLoggedInPayload;
import ma.naja7i.auth.events.payload.UserRegisteredPayload;
import ma.naja7i.auth.events.payload.UserSuspendedPayload;
import ma.naja7i.auth.infrastructure.messaging.KafkaEventPublisher;
import ma.naja7i.auth.infrastructure.persistence.AuditLogRepository;
import ma.naja7i.auth.infrastructure.persistence.UserRepository;
import ma.naja7i.auth.infrastructure.web.dto.LoginRequest;
import ma.naja7i.auth.infrastructure.web.dto.RegisterRequest;
import ma.naja7i.auth.infrastructure.web.dto.RegisterResponse;
import ma.naja7i.auth.infrastructure.web.dto.TokenResponse;
import ma.naja7i.shared.dto.Role;
import ma.naja7i.shared.errors.BusinessException;
import ma.naja7i.shared.errors.ErrorCode;
import ma.naja7i.shared.events.EventTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final TokenService tokenService;
    private final KafkaEventPublisher eventPublisher;

    @Transactional
    public RegisterResponse register(RegisterRequest req, String ip, String userAgent) {
        if (req.role() == Role.ADMIN) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN_ROLE,
                    "ADMIN accounts cannot be self-registered");
        }
        if (userRepository.existsByPhone(req.phone())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Phone already registered");
        }
        if (req.email() != null && !req.email().isBlank()
                && userRepository.existsByEmail(req.email())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Email already registered");
        }

        User user = User.builder()
                .phone(req.phone())
                .email(req.email() == null || req.email().isBlank() ? null : req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .role(req.role())
                .status(UserStatus.PENDING_OTP)
                .locale(req.localeOrDefault())
                .emailVerified(false)
                .phoneVerified(false)
                .twoFaEnabled(false)
                .build();
        user = userRepository.save(user);

        otpService.issueAndPublish(user, OtpChannel.SMS);
        audit(user.getId(), AuditAction.REGISTER, ip, userAgent);

        log.info("User registered, awaiting OTP verification (role={})", user.getRole());
        return new RegisterResponse(user.getId(), user.getStatus().name(),
                "OTP sent. Verify within 5 minutes.");
    }

    @Transactional
    public TokenResponse verifyOtp(UUID userId, String code, String ip, String userAgent) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new BusinessException(ErrorCode.AUTH_USER_SUSPENDED, "Account suspended");
        }
        otpService.verify(user, code);

        user.setStatus(UserStatus.ACTIVE);
        user.setPhoneVerified(true);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        TokenResponse tokens = tokenService.issueTokens(user, userAgent);
        audit(user.getId(), AuditAction.OTP_VERIFIED, ip, userAgent);

        eventPublisher.publish(
                EventTopics.AUTH_USER_REGISTERED,
                user.getId().toString(),
                "naja7i.auth.user.registered.v1",
                new UserRegisteredPayload(user.getId(), user.getRole(), user.getEmail(), user.getPhone()));

        return tokens;
    }

    @Transactional
    public void resendOtp(UUID userId, OtpChannel channel, String ip, String userAgent) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new BusinessException(ErrorCode.AUTH_USER_SUSPENDED, "Account suspended");
        }
        otpService.resend(user, channel);
        audit(user.getId(), AuditAction.OTP_RESENT, ip, userAgent);
    }

    @Transactional
    public TokenResponse login(LoginRequest req, String ip, String userAgent) {
        User user = findByPhoneOrEmail(req.phoneOrEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS,
                        "Invalid credentials"));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            audit(user.getId(), AuditAction.LOGIN_FAILURE, ip, userAgent);
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS, "Invalid credentials");
        }
        switch (user.getStatus()) {
            case SUSPENDED -> throw new BusinessException(ErrorCode.AUTH_USER_SUSPENDED, "Account suspended");
            case DELETED   -> throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS, "Account deleted");
            case PENDING_OTP -> throw new BusinessException(ErrorCode.AUTH_OTP_INVALID,
                    "Account not yet verified — please complete OTP first");
            case ACTIVE    -> { /* proceed */ }
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        TokenResponse tokens = tokenService.issueTokens(user, userAgent);
        audit(user.getId(), AuditAction.LOGIN_SUCCESS, ip, userAgent);

        eventPublisher.publish(
                EventTopics.AUTH_USER_LOGGED_IN,
                user.getId().toString(),
                "naja7i.auth.user.logged_in.v1",
                new UserLoggedInPayload(user.getId(), ip, userAgent, Instant.now()));

        return tokens;
    }

    @Transactional
    public void logout(UUID userId, String refreshToken, String ip, String userAgent) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            tokenService.revoke(refreshToken);
        }
        audit(userId, AuditAction.LOGOUT, ip, userAgent);
    }

    @Transactional
    public void suspend(UUID targetUserId, String reason, UUID actorId, String ip, String userAgent) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
        user.setStatus(UserStatus.SUSPENDED);
        userRepository.save(user);
        tokenService.revokeAllForUser(targetUserId);
        audit(actorId, AuditAction.SUSPENDED, ip, userAgent);

        eventPublisher.publish(
                EventTopics.AUTH_USER_SUSPENDED,
                user.getId().toString(),
                "naja7i.auth.user.suspended.v1",
                new UserSuspendedPayload(user.getId(), reason));
    }

    public User getById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
    }

    Optional<User> findByPhoneOrEmail(String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        if (value.contains("@")) {
            return userRepository.findByEmail(value);
        }
        return userRepository.findByPhone(value);
    }

    void audit(UUID userId, String action, String ip, String userAgent) {
        try {
            auditLogRepository.save(AuditLog.builder()
                    .userId(userId)
                    .action(action)
                    .ip(truncate(ip, 64))
                    .userAgent(truncate(userAgent, 255))
                    .build());
        } catch (Exception ex) {
            log.warn("Audit log write failed", ex);
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }
}
