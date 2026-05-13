package ma.naja7i.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "naja7i.cors")
public class CorsProperties {

    private List<String> allowedOrigins = List.of("http://localhost:5173", "http://localhost:3000");

    public List<String> getAllowedOrigins() { return allowedOrigins; }
    public void setAllowedOrigins(List<String> allowedOrigins) { this.allowedOrigins = allowedOrigins; }
}
