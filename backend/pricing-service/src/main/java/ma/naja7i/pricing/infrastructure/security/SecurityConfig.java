package ma.naja7i.pricing.infrastructure.security;

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
 * Trust model:
 * <ul>
 *   <li>{@code /api/v1/internal/**} → require {@code X-Internal-Key} (see {@link InternalKeyFilter}).</li>
 *   <li>All other authenticated routes → trust the gateway-supplied
 *       {@code X-User-Id} / {@code X-User-Role} headers (see {@link HeaderAuthFilter}).</li>
 *   <li>Public probes/docs are unauthenticated.</li>
 * </ul>
 *
 * <p>This service does NOT re-validate JWT signatures (that is the gateway's job).</p>
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final HeaderAuthFilter headerAuthFilter;
    private final InternalKeyFilter internalKeyFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .requestMatchers("/api/v1/internal/**").hasRole("INTERNAL")
                        .anyRequest().authenticated())
                // InternalKeyFilter runs first so internal callers can pass without gateway headers.
                .addFilterBefore(internalKeyFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(headerAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /** Disable the global filter registration — these filters only run inside the Spring Security chain. */
    @Bean
    public FilterRegistrationBean<HeaderAuthFilter> headerAuthFilterRegistration(HeaderAuthFilter filter) {
        FilterRegistrationBean<HeaderAuthFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }

    @Bean
    public FilterRegistrationBean<InternalKeyFilter> internalKeyFilterRegistration(InternalKeyFilter filter) {
        FilterRegistrationBean<InternalKeyFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }
}
