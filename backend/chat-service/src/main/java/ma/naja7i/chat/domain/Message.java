package ma.naja7i.chat.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A single chat message. Stored in {@code messages} collection.
 *
 * <p><b>Body handling</b> :
 * <ul>
 *   <li>{@code body} is the moderated payload delivered to clients (masked if the
 *       conversation was {@code PRE_ACCEPT} at send time).</li>
 *   <li>{@code bodyOriginal} is the as-typed payload, only accessible via ADMIN
 *       endpoints for anti-bypass investigation. Never logged.</li>
 * </ul>
 */
@Document(collection = "messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class Message {

    @Id
    @ToString.Include
    private UUID id;

    @Indexed
    @ToString.Include
    private UUID conversationId;

    @Indexed
    private UUID bookingId;

    private UUID senderId;
    private SenderRole senderRole;

    @ToString.Include
    private MessageType type;

    /** Possibly redacted by {@link ma.naja7i.chat.application.MessageModerator}. */
    private String body;

    /** Original (unmasked) body, ADMIN-only — must not be exposed via public DTOs. */
    private String bodyOriginal;

    @Builder.Default
    private List<Attachment> attachments = new ArrayList<>();

    @Builder.Default
    private Moderation moderation = Moderation.clean();

    @Builder.Default
    private List<ReadBy> readBy = new ArrayList<>();

    private Instant createdAt;
}
