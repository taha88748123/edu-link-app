package ma.naja7i.auth.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ma.naja7i.shared.security.JwtClaims;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Trusts the {@code X-User-Id} + {@code X-User-Role} headers injected by the API Gateway
 * (which performed JWT verification). Populates the Spring Security context so
 * {@code @PreAuthorize("hasRole('ADMIN')")} works on protected endpoints.
 *
 * No-op for public endpoints (register / login / verify-otp / refresh / password reset),
 * which never receive those headers.
 */
@Component
public class HeaderAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String userId = request.getHeader(JwtClaims.HEADER_USER_ID);
        String role   = request.getHeader(JwtClaims.HEADER_USER_ROLE);

        if (userId != null && role != null
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role)));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        chain.doFilter(request, response);
    }
}
