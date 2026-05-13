package ma.naja7i.shared.security;

/**
 * Canonical claim names embedded in NAJA7I JWTs.
 */
public final class JwtClaims {

    private JwtClaims() {}

    public static final String CLAIM_USER_ID = "uid";
    public static final String CLAIM_ROLE    = "role";
    public static final String CLAIM_LOCALE  = "locale";

    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_USER_ID       = "X-User-Id";
    public static final String HEADER_USER_ROLE     = "X-User-Role";
    public static final String HEADER_LOCALE        = "X-Locale";
    public static final String HEADER_TRACE_ID      = "X-Trace-Id";

    public static final String BEARER_PREFIX = "Bearer ";
}
