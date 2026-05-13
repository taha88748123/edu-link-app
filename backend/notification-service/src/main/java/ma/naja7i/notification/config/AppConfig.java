package ma.naja7i.notification.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(NotificationProperties.class)
public class AppConfig {
}
