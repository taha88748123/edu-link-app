package ma.naja7i.chat.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReadRequest(@NotNull UUID lastReadMessageId) {}
