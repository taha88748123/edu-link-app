package ma.naja7i.shared.security;

import ma.naja7i.shared.dto.Role;

import java.util.UUID;

/**
 * Immutable view of the authenticated principal, populated by the API Gateway
 * via {@code X-User-Id}, {@code X-User-Role}, {@code X-Locale} headers.
 */
public record UserContext(UUID userId, Role role, String locale, String traceId) {

    public boolean isAdmin()   { return role == Role.ADMIN; }
    public boolean isProf()    { return role == Role.PROF; }
    public boolean isStudent() { return role == Role.STUDENT; }
}
