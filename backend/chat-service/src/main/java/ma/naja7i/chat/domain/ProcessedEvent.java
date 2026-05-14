package ma.naja7i.chat.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

/**
 * Dedup row for at-least-once Kafka consumers. The Mongo {@code _id} is the eventId,
 * so a duplicate insert is rejected by the storage engine.
 */
@Document(collection = "processed_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEvent {

    @Id
    private UUID eventId;

    private String eventType;

    private Instant processedAt;
}
