package ma.naja7i.booking.application;

import ma.naja7i.booking.domain.Booking;
import ma.naja7i.booking.domain.BookingStateTransition;
import ma.naja7i.booking.domain.Dispute;
import ma.naja7i.booking.infrastructure.web.dto.BookingDetailedView;
import ma.naja7i.booking.infrastructure.web.dto.BookingTransitionView;
import ma.naja7i.booking.infrastructure.web.dto.BookingView;
import ma.naja7i.booking.infrastructure.web.dto.DisputeView;
import ma.naja7i.shared.dto.BookingStatus;
import ma.naja7i.shared.dto.Role;
import org.springframework.stereotype.Component;

/**
 * Builds masked DTO projections of {@link Booking} based on the viewer's role and the
 * booking's current status.
 *
 * <p>ANTI-BYPASS: no contact details before ACCEPTEE_PAR_PROF.
 *
 * <p>booking-service intentionally does NOT persist phone or email anywhere, so the
 * masking rule here is enforced by the simple fact that those fields do not exist on
 * the {@link BookingDetailedView} record. This class is the documented
 * single-source enforcement point if someone later tries to enrich responses with
 * contact info: any such enrichment MUST honour the {@code preAccept} guard below.
 */
@Component
public class BookingViewMasker {

    @SuppressWarnings("unused") // viewerRole reserved for future enrichment hooks
    public BookingView toListView(Booking b) {
        return new BookingView(
                b.getId(), b.getStudentId(), b.getProfId(), b.getSlotId(),
                b.getSubjectCode(), b.getLevel(), b.getModality(),
                b.getDurationMinutes(),
                b.getTotalAmountMinor(), b.getCurrency(),
                b.getStatus(),
                b.getScheduledStart(), b.getScheduledEnd(),
                b.getCreatedAt());
    }

    public BookingDetailedView toDetailedView(Booking b, Role viewerRole) {
        // ANTI-BYPASS: no contact details before ACCEPTEE_PAR_PROF
        // The DTO record schema is the strongest guarantee — phone/email simply
        // cannot be expressed. The pre-accept guard below hides reasons that
        // could leak the prof's internal decisioning.
        boolean preAccept = b.getStatus() == BookingStatus.DEMANDEE;
        String declineReason = preAccept ? null : b.getDeclineReason();

        // viewerRole is plumbed through for future hooks (e.g. ADMIN sees raw fields,
        // PROF sees its own decline reason). v1 has no divergence beyond preAccept.
        if (viewerRole == null) {
            viewerRole = Role.STUDENT;
        }
        return new BookingDetailedView(
                b.getId(), b.getStudentId(), b.getProfId(), b.getSlotId(),
                b.getSubjectCode(), b.getLevel(), b.getModality(),
                b.getDurationMinutes(),
                b.getAmountMinor(), b.getPlatformFeeMinor(), b.getTotalAmountMinor(),
                b.getCurrency(), b.getEscrowId(),
                b.getStatus(),
                b.getScheduledStart(), b.getScheduledEnd(),
                b.getActualStart(), b.getActualEnd(),
                b.getCancelReason(), b.getCancelledBy(), b.getCancelledAt(),
                b.getConfirmedAt(),
                declineReason, b.getDeclinedAt(),
                b.getCreatedAt(), b.getUpdatedAt());
    }

    public BookingTransitionView toTransitionView(BookingStateTransition t) {
        return new BookingTransitionView(
                t.getId(), t.getBookingId(), t.getFromStatus(), t.getToStatus(),
                t.getTriggeredBy(), t.getReason(), t.getCreatedAt());
    }

    public DisputeView toDisputeView(Dispute d) {
        return new DisputeView(
                d.getId(), d.getBookingId(), d.getOpenedBy(), d.getCause(),
                d.getEvidenceUrls(), d.getStatus(), d.getResolutionNotes(),
                d.getResolvedBy(), d.getResolvedAt(),
                d.getCreatedAt(), d.getUpdatedAt());
    }
}
