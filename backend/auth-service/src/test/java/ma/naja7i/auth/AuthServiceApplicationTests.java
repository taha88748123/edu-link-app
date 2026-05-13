package ma.naja7i.auth;

import org.junit.jupiter.api.Test;

/**
 * Smoke test that does NOT boot Spring (boot test requires PG + Redis + Kafka).
 * Full context loading is covered by {@code AuthControllerIT} which uses Testcontainers.
 */
class AuthServiceApplicationTests {

    @Test
    void mainClassIsPresent() {
        // sanity: the entry point class must exist on the classpath
        org.junit.jupiter.api.Assertions.assertNotNull(AuthServiceApplication.class);
    }
}
