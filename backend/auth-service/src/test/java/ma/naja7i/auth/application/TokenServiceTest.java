package ma.naja7i.auth.application;

import ma.naja7i.auth.config.JwtProperties;
import ma.naja7i.auth.domain.RefreshToken;
import ma.naja7i.auth.domain.User;
import ma.naja7i.auth.domain.UserStatus;
import ma.naja7i.auth.infrastructure.persistence.RefreshTokenRepository;
import ma.naja7i.auth.infrastructure.persistence.UserRepository;
import ma.naja7i.auth.infrastructure.web.dto.TokenResponse;
import ma.naja7i.shared.dto.Role;
import ma.naja7i.shared.errors.BusinessException;
import ma.naja7i.shared.errors.ErrorCode;
import ma.naja7i.shared.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock JwtService jwtService;
    @Mock JwtProperties jwtProperties;

    @InjectMocks TokenService tokenService;

    @Test
    void issueTokens_persistsHashedRefresh() {
        when(jwtProperties.getRefreshTtl()).thenReturn(Duration.ofDays(14));
        when(jwtProperties.getAccessTtl()).thenReturn(Duration.ofMinutes(15));
        User user = User.builder()
                .id(UUID.randomUUID()).phone("+212600000010")
                .role(Role.PROF).status(UserStatus.ACTIVE)
                .locale("fr").passwordHash("h").build();
        when(jwtService.issueAccessToken(user.getId(), Role.PROF, "fr"))
                .thenReturn("THE_ACCESS_TOKEN");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        TokenResponse tokens = tokenService.issueTokens(user, "JUnit");

        assertThat(tokens.accessToken()).isEqualTo("THE_ACCESS_TOKEN");
        assertThat(tokens.refreshToken()).isNotBlank();
        assertThat(tokens.expiresInSec()).isEqualTo(900L);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void rotate_invalidRefresh_throws() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> tokenService.rotate("bogus"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.AUTH_TOKEN_INVALID);
    }

    @Test
    void rotate_revokedRefresh_throwsExpired() {
        RefreshToken rt = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .tokenHash("hash")
                .expiresAt(Instant.now().plusSeconds(60))
                .revokedAt(Instant.now())
                .build();
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(rt));
        assertThatThrownBy(() -> tokenService.rotate("any"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.AUTH_TOKEN_EXPIRED);
    }
}
