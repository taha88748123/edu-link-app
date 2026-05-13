package ma.naja7i.payment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI paymentServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("NAJA7I — Payment Service API")
                        .description("""
                                Wallet, escrow, top-ups and payouts. All money figures are stored
                                in BIGINT centimes; the audit_log table is the immutable record of
                                every balance change.

                                Endpoints:
                                - STUDENT: balance, top-ups, audit
                                - PROF:    balance, payouts, audit
                                - ADMIN:   topup decisions, payout decisions, manual adjustments,
                                           audit export.
                                """)
                        .version("v1")
                        .license(new License().name("Internal — NAJA7I")));
    }
}
