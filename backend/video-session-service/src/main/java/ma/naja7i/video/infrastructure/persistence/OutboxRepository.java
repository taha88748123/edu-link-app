package ma.naja7i.video.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import ma.naja7i.video.domain.OutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("""
           SELECT o FROM OutboxEvent o
           WHERE o.publishedAt IS NULL
           ORDER BY o.createdAt ASC
           """)
    List<OutboxEvent> fetchPendingForPublishing(Pageable pageable);

    @Modifying
    @Query("UPDATE OutboxEvent o SET o.publishedAt = :ts WHERE o.id = :id")
    int markPublished(@Param("id") UUID id, @Param("ts") Instant ts);
}
