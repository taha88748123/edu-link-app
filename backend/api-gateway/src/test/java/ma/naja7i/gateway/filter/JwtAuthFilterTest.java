package ma.naja7i.gateway.filter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import ma.naja7i.gateway.config.PublicPaths;

class JwtAuthFilterTest {

    @Test
    void publicPaths_recognised() {
        assertThat(PublicPaths.isPublic("/actuator/health")).isTrue();
        assertThat(PublicPaths.isPublic("/api/v1/auth/login")).isTrue();
        assertThat(PublicPaths.isPublic("/api/v1/auth/register")).isTrue();
    }

    @Test
    void protectedPaths_notPublic() {
        assertThat(PublicPaths.isPublic("/api/v1/profile/me")).isFalse();
        assertThat(PublicPaths.isPublic("/api/v1/bookings")).isFalse();
        assertThat(PublicPaths.isPublic("/api/v1/payment/balance")).isFalse();
    }
}
