package ma.naja7i.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import ma.naja7i.shared.dto.Role;
import ma.naja7i.shared.errors.BusinessException;
import ma.naja7i.shared.errors.ErrorCode;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Issues & validates NAJA7I access tokens (HS256). Refresh tokens are handled in auth-service.
 * The same secret must be shared between auth-service and api-gateway.
 */
public class JwtService {

    private final SecretKey signingKey;
    private final Duration accessTtl;
    private final String issuer;

    public JwtService(String secret, Duration accessTtl, String issuer) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 bytes");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtl = accessTtl;
        this.issuer = issuer;
    }

    public String issueAccessToken(UUID userId, Role role, String locale) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(userId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTtl)))
                .claims(Map.of(
                        JwtClaims.CLAIM_USER_ID, userId.toString(),
                        JwtClaims.CLAIM_ROLE, role.name(),
                        JwtClaims.CLAIM_LOCALE, locale == null ? "fr" : locale
                ))
                .signWith(signingKey)
                .compact();
    }

    public UserContext verify(String token) {
        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token);
            Claims c = jws.getPayload();
            UUID userId = UUID.fromString(c.get(JwtClaims.CLAIM_USER_ID, String.class));
            Role role = Role.valueOf(c.get(JwtClaims.CLAIM_ROLE, String.class));
            String locale = c.get(JwtClaims.CLAIM_LOCALE, String.class);
            return new UserContext(userId, role, locale, UUID.randomUUID().toString());
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.AUTH_TOKEN_INVALID, e.getMessage());
        }
    }
}
