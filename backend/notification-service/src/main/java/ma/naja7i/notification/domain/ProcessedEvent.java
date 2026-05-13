package ma.naja7i.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Idempotency tracker for Kafka consumers. A row is inserted once per processed
 * event; on duplicate delivery the unique-key constraint short-circuits the handler.
 */
@Entity
@Table(name = "processed_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedEvent {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "topic", length = 128, nullable = false)
    private String topic;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private Instant processedAt;

    @PrePersist
    public void onCreate() {
        if (processedAt == null) processedAt = Instant.now();
    }
}
