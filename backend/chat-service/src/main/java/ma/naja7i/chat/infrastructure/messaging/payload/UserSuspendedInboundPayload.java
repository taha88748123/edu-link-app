package ma.naja7i.chat.infrastructure.messaging.payload;

import java.util.UUID;

public record UserSuspendedInboundPayload(UUID userId, String role, String reason) {
}
