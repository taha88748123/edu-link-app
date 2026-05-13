package ma.naja7i.pricing.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ma.naja7i.pricing.config.PricingProperties;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authenticates server-to-server callers (e.g. booking-service) by validating the
 * {@code X-Internal-Key} header against {@code naja7i.pricing.internal-key}. Only acts
 * on URIs under {@code /api/v1/internal/}.
 *
 * <p>On success, populates a Spring Security context with role {@code INTERNAL} so the
 * filter chain's {@code .authenticated()} check passes.</p>
 */
@Component
public class InternalKeyFilter extends OncePerRequestFilter {

    public static final String HEADER_INTERNAL_KEY = "X-Internal-Key";
    public static final String INTERNAL_ROUTE_PREFIX = "/api/v1/internal/";
    public static final String ROLE_INTERNAL = "ROLE_INTERNAL";

    private final PricingProperties props;

    public InternalKeyFilter(PricingProperties props) {
        this.props = props;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (request.getRequestURI() != null
                && request.getRequestURI().startsWith(INTERNAL_ROUTE_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            String presented = request.getHeader(HEADER_INTERNAL_KEY);
            String expected = props.getInternalKey();
            if (expected != null && presented != null && expected.equals(presented)) {
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                "internal", null,
                                List.of(new SimpleGrantedAuthority(ROLE_INTERNAL)));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
            // wrong/missing key: leave context empty -> Spring Security returns 401/403
        }

        chain.doFilter(request, response);
    }
}
