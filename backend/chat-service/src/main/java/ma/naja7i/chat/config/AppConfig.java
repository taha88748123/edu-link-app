package ma.naja7i.chat.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ma.naja7i.shared.errors.GlobalExceptionHandler;
import ma.naja7i.shared.security.JwtService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@Import(GlobalExceptionHandler.class)
@EnableConfigurationProperties({ChatProperties.class, JwtProperties.class})
public class AppConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return om;
    }

    /**
     * JWT verifier used by the STOMP CONNECT interceptor. Must use the SAME
     * secret as the gateway and auth-service.
     */
    @Bean
    public JwtService jwtService(JwtProperties props) {
        return new JwtService(props.getSecret(), props.getAccessTtl(), props.getIssuer());
    }

    /**
     * Plain {@link RestClient} used by the {@code anthropic} bypass analyser. We keep
     * timeouts short because the call is fire-and-forget (async) and the fallback
     * (logging) is always safe.
     */
    @Bean
    public RestClient anthropicRestClient(ChatProperties props) {
        Duration timeout = Duration.ofMillis(props.getAnthropic().getTimeoutMs());
        return RestClient.builder()
                .baseUrl(props.getAnthropic().getBaseUrl())
                .requestFactory(new org.springframework.http.client.SimpleClientHttpRequestFactory() {{
                    setConnectTimeout((int) timeout.toMillis());
                    setReadTimeout((int) timeout.toMillis());
                }})
                .build();
    }
}
