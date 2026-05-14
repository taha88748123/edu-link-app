package ma.naja7i.chat.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ma.naja7i.chat.config.ChatProperties;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authorises service-to-service calls hitting {@code /api/v1/internal/**} via the
 * shared {@code X-Internal-Key} header. On match, populates an authentication with
 * the synthetic role {@code ROLE_INTERNAL}.
 */
@Component
public class InternalKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Internal-Key";
    public static final String ROLE_INTERNAL = "ROLE_INTERNAL";

    private final ChatProperties props;

    public InternalKeyFilter(ChatProperties props) {
        this.props = props;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        if (req.getRequestURI() != null && req.getRequestURI().startsWith("/api/v1/internal/")) {
            String supplied = req.getHeader(HEADER);
            if (supplied != null && supplied.equals(props.getInternalKey())) {
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                "internal", null,
                                List.of(new SimpleGrantedAuthority(ROLE_INTERNAL)));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(req, res);
    }
}
