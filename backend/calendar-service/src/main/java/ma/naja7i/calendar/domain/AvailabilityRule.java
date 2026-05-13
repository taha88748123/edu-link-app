package ma.naja7i.calendar.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * A PROF-declared rule defining when they are available.
 *  - {@code WEEKLY}: recurs every week on {@code dayOfWeek}
 *  - {@code ONE_OFF}: applies once on {@code date}
 */
@Entity
@Table(name = "availability_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilityRule {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "prof_id", nullable = false, updatable = false)
    private UUID profId;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence", nullable = false, length = 16)
    private Recurrence recurrence;

    /** 0=Mon ... 6=Sun, NULL when {@code recurrence == ONE_OFF}. */
    @Column(name = "day_of_week")
    private Short dayOfWeek;

    /** NULL when {@code recurrence == WEEKLY}. */
    @Column(name = "date")
    private LocalDate date;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "timezone", nullable = false, length = 50)
    private String timezone;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (timezone == null) timezone = "Africa/Casablanca";
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
