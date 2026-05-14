package ma.naja7i.chat.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ma.naja7i.chat.domain.Attachment;
import ma.naja7i.chat.domain.MessageType;

import java.util.List;

public record CreateMessageRequest(
        @NotNull MessageType type,
        @Size(max = 4000) String body,
        List<Attachment> attachments
) {
}
