package ma.naja7i.gateway.config;

import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import java.util.List;

/**
 * Paths that bypass JWT verification.
 * Add to this list as new public-facing endpoints are introduced.
 */
public final class PublicPaths {

    private static final PathMatcher MATCHER = new AntPathMatcher();

    private static final List<String> PATTERNS = List.of(
            "/actuator/**",
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/verify-otp",
            "/api/v1/auth/refresh",
            "/api/v1/auth/password/reset-request",
            "/api/v1/auth/password/reset"
    );

    private PublicPaths() {}

    public static boolean isPublic(String path) {
        for (String p : PATTERNS) {
            if (MATCHER.match(p, path)) return true;
        }
        return false;
    }
}
