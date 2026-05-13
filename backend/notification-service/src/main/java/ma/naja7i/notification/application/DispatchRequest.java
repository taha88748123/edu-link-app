package ma.naja7i.notification.application;

import lombok.Builder;
import ma.naja7i.notification.domain.Channel;
import ma.naja7i.shared.dto.Role;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Internal value object describing a notification to dispatch. Built either from a
 * REST request or from a Kafka consumer.
 */
@Builder
public record DispatchRequest(
        UUID userId,
        Role role,                  // nullable — used to resolve channels when {@link #channels} is empty
        List<Channel> channels,     // optional override : if non-empty, exactly these channels are tried
        String templateKey,
        String locale,
        Map<String, Object> params
) {
}
