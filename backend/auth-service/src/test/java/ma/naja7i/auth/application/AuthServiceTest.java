package ma.naja7i.auth.application;

import ma.naja7i.auth.domain.OtpChannel;
import ma.naja7i.auth.domain.User;
import ma.naja7i.auth.domain.UserStatus;
import ma.naja7i.auth.events.payload.UserLoggedInPayload;
import ma.naja7i.auth.events.payload.UserRegisteredPayload;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock AuditLogRepository auditLogRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock OtpService otpService;
    @Mock TokenService tokenService;
    @Mock KafkaEventPublisher eventPublisher;

    @InjectMocks AuthService authService;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setup() {
        // baseline
    }

    @Test
    void register_happyPath_createsPendingUserAndPublishesOtp() {
        RegisterRequest req = new RegisterRequest("+212600000001", "student@example.com",
                "secret-pass", Role.STUDENT, "fr");
        when(userRepository.existsByPhone(req.phone())).thenReturn(false);
        when(userRepository.existsByEmail(req.email())).thenReturn(false);
        when(passwordEncoder.encode(req.password())).thenReturn("BCRYPT_HASH");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            if (u.getId() == null) u.setId(userId);
            if (u.getCreatedAt() == null) u.setCreatedAt(Instant.now());
            if (u.getUpdatedAt() == null) u.setUpdatedAt(Instant.now());
            return u;
        });

        RegisterResponse res = authService.register(req, "127.0.0.1", "junit");

        assertThat(res).isNotNull();
        assertThat(res.status()).isEqualTo("PENDING_OTP");
        assertThat(res.userId()).isEqualTo(userId);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getPhone()).isEqualTo("+212600000001");
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("BCRYPT_HASH");
        assertThat(saved.getValue().getStatus()).isEqualTo(UserStatus.PENDING_OTP);

        verify(otpService).issueAndPublish(any(User.class), eq(OtpChannel.SMS));
    }

    @Test
    void register_adminRole_isRejected() {
        RegisterRequest req = new RegisterRequest("+212600000002", null,
                "secret-pass", Role.ADMIN, "fr");
        assertThatThrownBy(() -> authService.register(req, "1.2.3.4", "ua"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.AUTH_FORBIDDEN_ROLE);
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_duplicatePhone_throwsValidationFailed() {
        RegisterRequest req = new RegisterRequest("+212600000003", null,
                "secret-pass", Role.STUDENT, "fr");
        when(userRepository.existsByPhone(req.phone())).thenReturn(true);
        assertThatThrownBy(() -> authService.register(req, "1.2.3.4", "ua"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.VALIDATION_FAILED);
    }

    @Test
    void verifyOtp_happyPath_activatesUserAndIssuesTokens() {
        User user = User.builder()
                .id(userId).phone("+212600000004").role(Role.STUDENT)
                .status(UserStatus.PENDING_OTP).locale("fr").passwordHash("h").build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tokenService.issueTokens(any(User.class), any()))
                .thenReturn(new TokenResponse("ACC", "REF", 900L, userId, Role.STUDENT));

        TokenResponse tokens = authService.verifyOtp(userId, "123456", "1.2.3.4", "ua");

        assertThat(tokens.accessToken()).isEqualTo("ACC");
        assertThat(tokens.refreshToken()).isEqualTo("REF");
        verify(otpService).verify(user, "123456");
        verify(userRepository).save(user);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.isPhoneVerified()).isTrue();
        verify(eventPublisher).publish(eq(EventTopics.AUTH_USER_REGISTERED),
                eq(userId.toString()), any(), any(UserRegisteredPayload.class));
    }

    @Test
    void verifyOtp_userMissing_throwsResourceNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.verifyOtp(userId, "123456", "ip", "ua"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    void login_happyPath_returnsTokens() {
        User user = User.builder()
                .id(userId).phone("+212600000005").role(Role.STUDENT)
                .status(UserStatus.ACTIVE).locale("fr").passwordHash("BCRYPT").build();
        when(userRepository.findByPhone("+212600000005")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("good-pass", "BCRYPT")).thenReturn(true);
        when(tokenService.issueTokens(any(User.class), any()))
                .thenReturn(new TokenResponse("ACC", "REF", 900L, userId, Role.STUDENT));

        TokenResponse out = authService.login(
                new LoginRequest("+212600000005", "good-pass"), "1.2.3.4", "ua");

        assertThat(out.accessToken()).isEqualTo("ACC");
        verify(eventPublisher).publish(eq(EventTopics.AUTH_USER_LOGGED_IN),
                eq(userId.toString()), any(), any(UserLoggedInPayload.class));
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials() {
        User user = User.builder()
                .id(userId).phone("+212600000006").role(Role.STUDENT)
                .status(UserStatus.ACTIVE).locale("fr").passwordHash("BCRYPT").build();
        when(userRepository.findByPhone("+212600000006")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bad-pass", "BCRYPT")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("+212600000006", "bad-pass"), "ip", "ua"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.AUTH_INVALID_CREDENTIALS);
    }

    @Test
    void login_suspendedAccount_throwsSuspended() {
        User user = User.builder()
                .id(userId).phone("+212600000007").role(Role.STUDENT)
                .status(UserStatus.SUSPENDED).locale("fr").passwordHash("BCRYPT").build();
        when(userRepository.findByPhone("+212600000007")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("good", "BCRYPT")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("+212600000007", "good"), "ip", "ua"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.AUTH_USER_SUSPENDED);
    }

    @Test
    void login_pendingOtpAccount_throwsOtpInvalid() {
        User user = User.builder()
                .id(userId).phone("+212600000008").role(Role.STUDENT)
                .status(UserStatus.PENDING_OTP).locale("fr").passwordHash("BCRYPT").build();
        when(userRepository.findByPhone("+212600000008")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("good", "BCRYPT")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("+212600000008", "good"), "ip", "ua"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.AUTH_OTP_INVALID);
    }
}
