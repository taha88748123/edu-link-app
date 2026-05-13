package ma.naja7i.auth.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security policy for auth-service.
 *
 * Important: this service does NOT re-validate the JWT — that is the gateway's job.
 * We trust the {@code X-User-Id} / {@code X-User-Role} headers populated by
 * {@link HeaderAuthFilter} and rely on method-level {@code @PreAuthorize}.
 *
 * Public endpoints (register, login, verify-otp, resend-otp, refresh, password reset)
 * are not authenticated at all — they're routed straight from the gateway without
 * any auth header.
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final HeaderAuthFilter headerAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable) // CORS handled at the gateway
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/verify-otp",
                                "/api/v1/auth/resend-otp",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/password/reset-request",
                                "/api/v1/auth/password/reset"
                        ).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(headerAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /** Prevents Spring Boot from registering {@link HeaderAuthFilter} on the global servlet
     *  filter chain — it must only run inside the Spring Security chain. */
    @Bean
    public FilterRegistrationBean<HeaderAuthFilter> headerAuthFilterRegistration(HeaderAuthFilter filter) {
        FilterRegistrationBean<HeaderAuthFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }
}
