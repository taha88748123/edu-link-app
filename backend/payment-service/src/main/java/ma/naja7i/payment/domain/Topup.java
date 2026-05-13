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
@Table(name = "topup_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Topup {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 20, updatable = false)
    private TopupMethod method;

    @Column(name = "amount_minor", nullable = false, updatable = false)
    private Long amountMinor;

    @Column(name = "approved_amount_minor")
    private Long approvedAmountMinor;

    @Column(name = "currency", nullable = false, length = 3, updatable = false)
    private String currency;

    @Column(name = "proof_url", length = 500, updatable = false)
    private String proofUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TopupStatus status;

    @Column(name = "admin_notes")
    private String adminNotes;

    @Column(name = "decided_by")
    private UUID decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (currency == null) currency = "MAD";
        if (version == null) version = 0;
    }
}
