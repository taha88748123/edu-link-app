package ma.naja7i.ai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ma.naja7i.shared.errors.GlobalExceptionHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Wires the shared {@link GlobalExceptionHandler} so business errors map to
 * the canonical {@code ApiResponse.ApiError} envelope, and exposes the
 * Kafka-aware {@link ObjectMapper} used by both producer and outbox payloads.
 */
@Configuration
@Import(GlobalExceptionHandler.class)
@EnableConfigurationProperties(AiProperties.class)
public class AppConfig {

    @Bean
    public ObjectMapper kafkaObjectMapper() {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return om;
    }
}
