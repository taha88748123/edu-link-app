package ma.naja7i.calendar.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ma.naja7i.shared.dto.Modality;

import java.time.Instant;
import java.util.UUID;

/**
 * Materialised bookable slot. Lifecycle:
 *   AVAILABLE → RESERVED_PENDING → BOOKED  (happy path via booking-service)
 *   RESERVED_PENDING → AVAILABLE          (release: TTL expired or explicit)
 *   BOOKED → AVAILABLE                    (booking cancellation)
 *   AVAILABLE ↔ BLOCKED                   (PROF blocks the slot)
 *
 * <p>Optimistic locking via {@link Version}: concurrent reservations on the same slot
 * lose with an {@link org.springframework.orm.ObjectOptimisticLockingFailureException}
 * which the service translates into {@code CALENDAR_SLOT_CONFLICT}.
 */
@Entity
@Table(name = "slots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Slot {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "prof_id", nullable = false, updatable = false)
    private UUID profId;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "modality", nullable = false, length = 20)
    private Modality modality;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SlotStatus status;

    @Column(name = "booking_id")
    private UUID bookingId;

    @Column(name = "generated_from_rule_id")
    private UUID generatedFromRuleId;

    /** When status=RESERVED_PENDING, after this instant the slot is auto-released. */
    @Column(name = "reserved_until")
    private Instant reservedUntil;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (version == null) version = 0;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
