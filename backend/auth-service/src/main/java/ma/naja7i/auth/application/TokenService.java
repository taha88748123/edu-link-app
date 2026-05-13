package ma.naja7i.auth.application;

import lombok.RequiredArgsConstructor;
import ma.naja7i.auth.config.JwtProperties;
import ma.naja7i.auth.domain.RefreshToken;
import ma.naja7i.auth.domain.User;
import ma.naja7i.auth.domain.UserStatus;
import ma.naja7i.auth.infrastructure.persistence.RefreshTokenRepository;
import ma.naja7i.auth.infrastructure.persistence.UserRepository;
import ma.naja7i.auth.infrastructure.web.dto.TokenResponse;
import ma.naja7i.shared.errors.BusinessException;
import ma.naja7i.shared.errors.ErrorCode;
import ma.naja7i.shared.security.JwtService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenService {

    private static final SecureRandom RNG = new SecureRandom();
    private static final int REFRESH_TOKEN_BYTES = 32; // 256 bits

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    /** Generates a new pair (access + refresh) for a user. Stores hashed refresh in DB. */
    @Transactional
    public TokenResponse issueTokens(User user, String deviceLabel) {
        String access = jwtService.issueAccessToken(user.getId(), user.getRole(), user.getLocale());
        String refresh = generateRefreshToken();
        RefreshToken token = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(sha256Hex(refresh))
                .expiresAt(Instant.now().plus(jwtProperties.getRefreshTtl()))
                .deviceLabel(deviceLabel)
                .build();
        refreshTokenRepository.save(token);
        return new TokenResponse(
                access,
                refresh,
                jwtProperties.getAccessTtl().toSeconds(),
                user.getId(),
                user.getRole());
    }

    /** Rotates a refresh token: validates the old one, revokes it, issues a fresh pair. */
    @Transactional
    public TokenResponse rotate(String presentedRefresh) {
        String hash = sha256Hex(presentedRefresh);
        RefreshToken existing = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_TOKEN_INVALID, "Unknown refresh token"));
        if (!existing.isActive()) {
            throw new BusinessException(ErrorCode.AUTH_TOKEN_EXPIRED, "Refresh token expired or revoked");
        }
        User user = userRepository.findById(existing.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_TOKEN_INVALID, "Owner not found"));
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new BusinessException(ErrorCode.AUTH_USER_SUSPENDED, "Account suspended");
        }
        existing.setRevokedAt(Instant.now());
        refreshTokenRepository.save(existing);
        return issueTokens(user, existing.getDeviceLabel());
    }

    /** Revokes a single refresh token (logout). */
    @Transactional
    public void revoke(String refresh) {
        refreshTokenRepository.findByTokenHash(sha256Hex(refresh))
                .ifPresent(rt -> {
                    if (rt.getRevokedAt() == null) {
                        rt.setRevokedAt(Instant.now());
                        refreshTokenRepository.save(rt);
                    }
                });
    }

    /** Revokes every refresh token owned by a user (used on password reset / suspension). */
    @Transactional
    public int revokeAllForUser(UUID userId) {
        return refreshTokenRepository.revokeAllForUser(userId, Instant.now());
    }

    public String generateRefreshToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        RNG.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String sha256Hex(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
