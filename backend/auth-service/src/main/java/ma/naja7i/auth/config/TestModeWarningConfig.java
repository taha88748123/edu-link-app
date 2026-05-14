package ma.naja7i.auth.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Emits a loud WARN at startup whenever the dev-only test mode is enabled.
 * Operationally this is the canary log entry that surfaces if the property
 * is accidentally left on in a non-dev environment.
 */
@Configuration
@ConditionalOnProperty(prefix = "naja7i.auth.dev", name = "test-mode", havingValue = "true")
public class TestModeWarningConfig {

    private static final Logger log = LoggerFactory.getLogger(TestModeWarningConfig.class);

    @PostConstruct
    void warn() {
        log.warn("==========================================================================");
        log.warn("WARN: naja7i.auth.dev.test-mode=true — DO NOT ENABLE IN PRODUCTION");
        log.warn("The /api/v1/dev/auth/** endpoints are now active for the E2E harness.");
        log.warn("==========================================================================");
    }
}
