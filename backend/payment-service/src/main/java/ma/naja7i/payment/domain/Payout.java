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

@Entity
@Table(name = "payouts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payout {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "prof_id", nullable = false, updatable = false)
    private UUID profId;

    @Column(name = "requested_amount_minor", nullable = false, updatable = false)
    private Long requestedAmountMinor;

    @Column(name = "approved_amount_minor")
    private Long approvedAmountMinor;

    @Column(name = "currency", nullable = false, length = 3, updatable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PayoutStatus status;

    @Column(name = "iban_ref", length = 34)
    private String ibanRef;

    @Column(name = "payment_ref", length = 100)
    private String paymentRef;

    @Column(name = "admin_notes")
    private String adminNotes;

    @Column(name = "decided_by")
    private UUID decidedBy;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @PrePersist
    void onCreate() {
        if (requestedAt == null) requestedAt = Instant.now();
        if (currency == null) currency = "MAD";
        if (version == null) version = 0;
    }
}
