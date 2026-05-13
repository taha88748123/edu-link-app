package ma.naja7i.booking.infrastructure.rest;

import ma.naja7i.booking.config.BookingProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Registers a {@link RestClient.Builder} bean that is Eureka-aware via
 * {@link LoadBalanced}, so callers can use {@code http://calendar-service} or
 * {@code http://pricing-service} and the LoadBalancer interceptor will resolve to
 * actual instances at request time.
 */
@Configuration
public class RestClientConfig {

    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder(BookingProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(properties.getRestConnectTimeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(properties.getRestReadTimeoutMs()));
        return RestClient.builder().requestFactory(factory);
    }
}
