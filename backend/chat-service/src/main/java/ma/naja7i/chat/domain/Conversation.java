package ma.naja7i.chat.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

/**
 * A chat conversation, scoped to one booking and exactly two participants
 * (one {@code PROF} and one {@code STUDENT}). Stored in collection {@code conversations}.
 */
@Document(collection = "conversations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {

    @Id
    private UUID id;

    /** Unique on booking — see {@code MongoConfig} for the index. */
    @Indexed(unique = true)
    private UUID bookingId;

    private UUID profId;
    private UUID studentId;

    private ConversationStatus status;

    private Instant lastMessageAt;
    private Instant createdAt;
    private Instant closedAt;

    @Builder.Default
    private UnreadCounts unreadCounts = UnreadCounts.zero();

    public boolean isParticipant(UUID userId) {
        return userId != null && (userId.equals(profId) || userId.equals(studentId));
    }
}
