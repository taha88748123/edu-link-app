package ma.naja7i.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Escrow row: one per locked booking. The student's money is moved out of their
 * wallet into the escrow when LOCKED, and back out (to prof or to student) on
 * release. Unique on {@code bookingId} so a duplicate {@code booking.requested}
 * cannot lock twice.
 */
@Entity
@Table(name = "escrows")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Escrow {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "booking_id", nullable = false, unique = true, updatable = false)
    private UUID bookingId;

    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;

    @Column(name = "prof_id", nullable = false, updatable = false)
    private UUID profId;

    @Column(name = "amount_minor", nullable = false, updatable = false)
    private Long amountMinor;

    @Column(name = "platform_fee_minor", nullable = false, updatable = false)
    private Long platformFeeMinor;

    @Column(name = "prof_share_minor", nullable = false, updatable = false)
    private Long profShareMinor;

    @Column(name = "currency", nullable = false, length = 3, updatable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private EscrowStatus status;

    @Column(name = "student_refund_minor")
    private Long studentRefundMinor;

    @Column(name = "prof_payout_minor")
    private Long profPayoutMinor;

    @Column(name = "locked_at", nullable = false, updatable = false)
    private Instant lockedAt;

    @Column(name = "released_at")
    private Instant releasedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @PrePersist
    void onCreate() {
        if (lockedAt == null) lockedAt = Instant.now();
        if (currency == null) currency = "MAD";
        if (version == null) version = 0;
    }
}
