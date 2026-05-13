package ma.naja7i.auth.application;

import ma.naja7i.auth.domain.User;
import ma.naja7i.auth.domain.UserStatus;
import ma.naja7i.auth.events.payload.PasswordResetRequestedPayload;
import ma.naja7i.auth.infrastructure.messaging.KafkaEventPublisher;
import ma.naja7i.auth.infrastructure.persistence.UserRepository;
import ma.naja7i.auth.infrastructure.redis.PasswordResetTokenStore;
import ma.naja7i.shared.dto.Role;
import ma.naja7i.shared.errors.BusinessException;
import ma.naja7i.shared.errors.ErrorCode;
import ma.naja7i.shared.events.EventTopics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordResetTokenStore tokenStore;
    @Mock PasswordEncoder passwordEncoder;
    @Mock TokenService tokenService;
    @Mock AuthService authService;
    @Mock KafkaEventPublisher eventPublisher;

    @InjectMocks PasswordResetService service;

    @Test
    void requestReset_publishesEvent_whenUserExists() {
        UUID id = UUID.randomUUID();
        User user = User.builder().id(id).phone("+212600000020").role(Role.STUDENT)
                .status(UserStatus.ACTIVE).locale("fr").passwordHash("h").build();
        when(userRepository.findByPhone("+212600000020")).thenReturn(Optional.of(user));
        when(tokenStore.ttl()).thenReturn(Duration.ofMinutes(30));

        service.requestReset("+212600000020", "1.2.3.4", "ua");

        verify(tokenStore).store(any(String.class), eq(id));
        verify(eventPublisher).publish(eq(EventTopics.AUTH_PASSWORD_RESET),
                eq(id.toString()), any(), any(PasswordResetRequestedPayload.class));
    }

    @Test
    void requestReset_isSilent_whenUserUnknown() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());
        service.requestReset("ghost@example.com", "1.2.3.4", "ua");
        verify(tokenStore, never()).store(any(), any());
        verify(eventPublisher, never()).publish(any(), any(), any(), any());
    }

    @Test
    void resetPassword_revokesAllTokensAndUpdatesPassword() {
        UUID id = UUID.randomUUID();
        User user = User.builder().id(id).phone("+212600000021").role(Role.STUDENT)
                .status(UserStatus.ACTIVE).locale("fr").passwordHash("old").build();
        when(tokenStore.consume("tok")).thenReturn(Optional.of(id));
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("brand-new-pass")).thenReturn("new-bcrypt");

        service.resetPassword("tok", "brand-new-pass", "1.2.3.4", "ua");

        verify(userRepository).save(user);
        verify(tokenService).revokeAllForUser(id);
    }

    @Test
    void resetPassword_invalidToken_throws() {
        when(tokenStore.consume("bad")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.resetPassword("bad", "pass", "ip", "ua"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.AUTH_TOKEN_INVALID);
    }
}
