package ma.naja7i.chat.infrastructure.persistence;

import ma.naja7i.chat.domain.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends MongoRepository<Message, UUID> {

    /** Most-recent-first listing for a conversation (page-based fallback). */
    List<Message> findByConversationIdOrderByCreatedAtDesc(UUID conversationId, Pageable pageable);

    /** Cursor pagination: messages strictly older than {@code before}. */
    List<Message> findByConversationIdAndCreatedAtBeforeOrderByCreatedAtDesc(
            UUID conversationId, Instant before, Pageable pageable);

    /** Used by the read-receipts handler: all messages in conv at or before a moment. */
    List<Message> findByConversationIdAndCreatedAtLessThanEqualOrderByCreatedAtAsc(
            UUID conversationId, Instant cutoff);
}
