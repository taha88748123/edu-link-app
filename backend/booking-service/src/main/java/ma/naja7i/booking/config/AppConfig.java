package ma.naja7i.booking.config;

import ma.naja7i.shared.errors.GlobalExceptionHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(GlobalExceptionHandler.class)
@EnableConfigurationProperties({BookingProperties.class})
public class AppConfig {
}
