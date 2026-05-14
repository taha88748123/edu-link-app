package ma.naja7i.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ma.naja7i.shared.events.DomainEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean
    public ObjectMapper kafkaObjectMapper() {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return om;
    }

    @Bean
    public ProducerFactory<String, DomainEvent<?>> producerFactory(ObjectMapper kafkaObjectMapper) {
        // Spring Kafka refuses a JsonSerializer that is both pre-configured (setters
        // on the instance) AND configured via producer properties (e.g.
        // JsonSerializer.ADD_TYPE_INFO_HEADERS). We pre-configure the instance, so
        // the props map must NOT contain JsonSerializer.* keys.
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        JsonSerializer<DomainEvent<?>> valueSer = new JsonSerializer<>(kafkaObjectMapper);
        valueSer.setAddTypeInfo(false);
        return new DefaultKafkaProducerFactory<>(props, new StringSerializer(), valueSer);
    }

    @Bean
    public KafkaTemplate<String, DomainEvent<?>> kafkaTemplate(
            ProducerFactory<String, DomainEvent<?>> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}
