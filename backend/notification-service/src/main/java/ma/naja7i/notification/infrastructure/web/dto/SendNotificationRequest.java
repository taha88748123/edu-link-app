package ma.naja7i.notification.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ma.naja7i.notification.domain.Channel;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Body accepted by {@code POST /api/v1/notifications/send}. Direct send is used
 * for ad-hoc admin messages and for E2E testing without going through Kafka.
 */
public record SendNotificationRequest(
        @NotNull UUID userId,
        List<Channel> channels,
        @NotBlank String templateKey,
        String locale,
        Map<String, Object> params
) {
}
