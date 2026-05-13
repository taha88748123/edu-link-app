package ma.naja7i.auth.config;

import ma.naja7i.shared.security.JwtService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableConfigurationProperties({JwtProperties.class, OtpProperties.class})
public class AppConfig {

    /** bcrypt strength 12 — required by the brief. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public JwtService jwtService(JwtProperties props) {
        return new JwtService(props.getSecret(), props.getAccessTtl(), props.getIssuer());
    }
}
