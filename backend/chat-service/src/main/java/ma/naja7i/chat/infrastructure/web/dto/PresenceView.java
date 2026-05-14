package ma.naja7i.chat.infrastructure.web.dto;

import java.util.UUID;

public record PresenceView(UUID userId, boolean online, Long ttlSeconds) {}
