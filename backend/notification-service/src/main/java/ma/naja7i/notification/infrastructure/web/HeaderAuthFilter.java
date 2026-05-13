package ma.naja7i.notification.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import ma.naja7i.shared.dto.Role;
import ma.naja7i.shared.security.JwtClaims;
import ma.naja7i.shared.security.UserContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Reads the {@code X-User-Id / X-User-Role / X-Locale / X-Trace-Id} headers injected
 * by the API gateway and exposes a {@link UserContext} via the request scope. The
 * filter does NOT enforce authentication on its own — service-to-service traffic
 * may bypass it by carrying {@code X-Internal-Key} (handled by {@link InternalKeyFilter}).
 */
@Component
@Slf4j
public class HeaderAuthFilter extends OncePerRequestFilter {

    public static final String ATTR_USER_CONTEXT = "naja7i.userContext";

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String userId = req.getHeader(JwtClaims.HEADER_USER_ID);
        String role   = req.getHeader(JwtClaims.HEADER_USER_ROLE);
        String locale = req.getHeader(JwtClaims.HEADER_LOCALE);
        String trace  = req.getHeader(JwtClaims.HEADER_TRACE_ID);
        if (userId != null && role != null) {
            try {
                UserContext ctx = new UserContext(
                        UUID.fromString(userId),
                        Role.valueOf(role),
                        locale == null ? "fr" : locale,
                        trace == null ? UUID.randomUUID().toString() : trace);
                req.setAttribute(ATTR_USER_CONTEXT, ctx);
            } catch (IllegalArgumentException ex) {
                log.warn("Ignoring bad gateway headers : userId={} role={}", userId, role);
            }
        }
        if (trace != null) {
            RequestContextHolder.currentRequestAttributes()
                    .setAttribute("X-Trace-Id", trace, RequestAttributes.SCOPE_REQUEST);
        }
        chain.doFilter(req, res);
    }
}
