package ma.naja7i.chat.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ma.naja7i.shared.dto.Role;
import ma.naja7i.shared.security.JwtClaims;
import ma.naja7i.shared.security.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Reads gateway-injected {@code X-User-Id / X-User-Role / X-Locale / X-Trace-Id}
 * headers and populates Spring's SecurityContext with a {@link UserContext}.
 *
 * <p>Trust model: the gateway has already verified the JWT signature. This service
 * trusts the headers verbatim and does not re-validate.
 */
@Component
public class HeaderAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(HeaderAuthFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String userId  = request.getHeader(JwtClaims.HEADER_USER_ID);
        String role    = request.getHeader(JwtClaims.HEADER_USER_ROLE);
        String locale  = request.getHeader(JwtClaims.HEADER_LOCALE);
        String traceId = request.getHeader(JwtClaims.HEADER_TRACE_ID);

        if (traceId != null) {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                attrs.setAttribute(JwtClaims.HEADER_TRACE_ID, traceId,
                        ServletRequestAttributes.SCOPE_REQUEST);
            }
        }

        if (userId != null && role != null
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UUID uid = UUID.fromString(userId);
                Role r = Role.valueOf(role.toUpperCase());
                UserContext ctx = new UserContext(uid, r,
                        locale == null ? "fr" : locale,
                        traceId == null ? UUID.randomUUID().toString() : traceId);
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                ctx, null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + r.name())));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception ex) {
                // Never log header values themselves (potential PII).
                log.debug("Invalid auth headers: {}", ex.getMessage());
            }
        }

        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
