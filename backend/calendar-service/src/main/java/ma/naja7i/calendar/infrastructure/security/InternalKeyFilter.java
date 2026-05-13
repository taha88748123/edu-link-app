package ma.naja7i.calendar.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ma.naja7i.calendar.config.CalendarProperties;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Service-to-service auth for {@code /api/v1/internal/**}.
 *
 * <p>The booking-service supplies a static shared secret in {@code X-Internal-Key}.
 * v2 will replace this with mTLS, but in v1 this is sufficient since these endpoints
 * are never exposed by the API Gateway to external clients.
 */
@Component
public class InternalKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Internal-Key";
    public static final String ROLE   = "INTERNAL_SERVICE";

    private final CalendarProperties props;

    public InternalKeyFilter(CalendarProperties props) {
        this.props = props;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/internal/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String supplied = request.getHeader(HEADER);
        if (supplied == null || !supplied.equals(props.getInternalKey())) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":{\"code\":\"AUTH_TOKEN_INVALID\",\"message\":\"Missing or invalid X-Internal-Key\"}}");
            return;
        }
        // Grant a synthetic authority so @PreAuthorize / authorizeHttpRequests can match it.
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "internal", null, List.of(new SimpleGrantedAuthority("ROLE_" + ROLE)));
        SecurityContextHolder.getContext().setAuthentication(auth);
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
