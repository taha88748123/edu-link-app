package ma.naja7i.calendar.infrastructure.persistence;

import ma.naja7i.calendar.domain.Slot;
import ma.naja7i.calendar.domain.SlotStatus;
import ma.naja7i.shared.dto.Modality;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SlotRepository extends JpaRepository<Slot, UUID> {

    Optional<Slot> findByProfIdAndStartAt(UUID profId, Instant startAt);

    /** Public catalog query: returns AVAILABLE slots only. */
    @Query("""
           SELECT s FROM Slot s
            WHERE s.profId = :profId
              AND s.status = ma.naja7i.calendar.domain.SlotStatus.AVAILABLE
              AND s.startAt >= :from
              AND s.startAt <  :to
              AND (:modality IS NULL OR s.modality = :modality)
           ORDER BY s.startAt
           """)
    Page<Slot> findAvailableForProf(@Param("profId") UUID profId,
                                    @Param("from") Instant from,
                                    @Param("to") Instant to,
                                    @Param("modality") Modality modality,
                                    Pageable pageable);

    /** PROF dashboard query (any status). */
    @Query("""
           SELECT s FROM Slot s
            WHERE s.profId = :profId
              AND s.startAt >= :from
              AND s.startAt <  :to
              AND (:status IS NULL OR s.status = :status)
           ORDER BY s.startAt
           """)
    Page<Slot> findForProf(@Param("profId") UUID profId,
                           @Param("from") Instant from,
                           @Param("to") Instant to,
                           @Param("status") SlotStatus status,
                           Pageable pageable);

    /** Used by {@link ma.naja7i.calendar.application.SlotExpiryJob}. */
    List<Slot> findByStatusAndReservedUntilLessThan(SlotStatus status, Instant cutoff);

    /** Used when a rule is soft-deleted: future AVAILABLE slots from this rule become BLOCKED. */
    List<Slot> findByGeneratedFromRuleIdAndStatusAndStartAtGreaterThan(
            UUID ruleId, SlotStatus status, Instant after);

    /** Used when a prof is suspended/rejected: every future slot is taken out of the catalog. */
    List<Slot> findByProfIdAndStartAtGreaterThanAndStatusIn(
            UUID profId, Instant after, List<SlotStatus> statuses);
}
