package ma.naja7i.chat.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;
import ma.naja7i.chat.domain.ConversationStatus;

import java.util.UUID;

public record CreateConversationRequest(
        @NotNull UUID bookingId,
        @NotNull UUID profId,
        @NotNull UUID studentId,
        ConversationStatus status
) {
}
