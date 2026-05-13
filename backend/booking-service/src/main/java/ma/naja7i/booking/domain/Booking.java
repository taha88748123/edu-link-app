package ma.naja7i.booking.domain;

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
import ma.naja7i.shared.dto.BookingStatus;
import ma.naja7i.shared.dto.Modality;
import ma.naja7i.shared.dto.SchoolLevel;

import java.time.Instant;
import java.util.UUID;

/**
 * Booking aggregate (one row per session contracted by a student with a prof).
 *
 * <p>Lifecycle:
 * <pre>
 *   DEMANDEE  -> ACCEPTEE_PAR_PROF -> EN_COURS  -> TERMINEE -> CONFIRMEE
 *      \\           \\                   \\           \\
 *       ANNULEE      ANNULEE             LITIGE      LITIGE       LITIGE (within window)
 * </pre>
 *
 * <p>Money: BIGINT centimes (MAD has 2 decimals). Never use float/double.
 * <p>Optimistic locking via {@link Version}: concurrent transitions lose.
 */
@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;

    @Column(name = "prof_id", nullable = false, updatable = false)
    private UUID profId;

    @Column(name = "slot_id", nullable = false, updatable = false)
    private UUID slotId;

    @Column(name = "subject_code", length = 20)
    private String subjectCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", length = 20)
    private SchoolLevel level;

    @Enumerated(EnumType.STRING)
    @Column(name = "modality", length = 20)
    private Modality modality;

    @Column(name = "duration_minutes")
    private Short durationMinutes;

    @Column(name = "amount_minor", nullable = false)
    private Long amountMinor;

    @Column(name = "platform_fee_minor", nullable = false)
    private Long platformFeeMinor;

    @Column(name = "total_amount_minor", nullable = false)
    private Long totalAmountMinor;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "escrow_id")
    private UUID escrowId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private BookingStatus status;

    @Column(name = "scheduled_start", nullable = false)
    private Instant scheduledStart;

    @Column(name = "scheduled_end", nullable = false)
    private Instant scheduledEnd;

    @Column(name = "actual_start")
    private Instant actualStart;

    @Column(name = "actual_end")
    private Instant actualEnd;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Column(name = "cancelled_by")
    private UUID cancelledBy;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "decline_reason")
    private String declineReason;

    @Column(name = "declined_at")
    private Instant declinedAt;

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
        if (currency == null) currency = "MAD";
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
