package ma.naja7i.chat.infrastructure.persistence;

import ma.naja7i.chat.domain.Conversation;
import ma.naja7i.chat.domain.ConversationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends MongoRepository<Conversation, UUID> {

    Optional<Conversation> findByBookingId(UUID bookingId);

    /** Conversations where the user is either prof or student, filtered by status if provided. */
    Page<Conversation> findByProfIdOrStudentIdAndStatus(UUID profId, UUID studentId,
                                                       ConversationStatus status, Pageable pageable);

    Page<Conversation> findByProfIdOrStudentId(UUID profId, UUID studentId, Pageable pageable);

    /** Admin search by any combination of booking / prof / student. */
    Page<Conversation> findByBookingId(UUID bookingId, Pageable pageable);

    Page<Conversation> findByProfId(UUID profId, Pageable pageable);

    Page<Conversation> findByStudentId(UUID studentId, Pageable pageable);

    /** Used when an auth.user.suspended event needs to close every conversation. */
    List<Conversation> findByProfIdOrStudentId(UUID profId, UUID studentId);
}
