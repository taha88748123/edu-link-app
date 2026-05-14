package ma.naja7i.video.config;

import ma.naja7i.shared.errors.GlobalExceptionHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(GlobalExceptionHandler.class)
@EnableConfigurationProperties({VideoProperties.class})
public class AppConfig {
}
