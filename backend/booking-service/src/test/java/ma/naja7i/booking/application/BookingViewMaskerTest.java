package ma.naja7i.booking.application;

import ma.naja7i.booking.domain.Booking;
import ma.naja7i.booking.infrastructure.web.dto.BookingDetailedView;
import ma.naja7i.shared.dto.BookingStatus;
import ma.naja7i.shared.dto.Modality;
import ma.naja7i.shared.dto.Role;
import ma.naja7i.shared.dto.SchoolLevel;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BookingViewMaskerTest {

    private final BookingViewMasker masker = new BookingViewMasker();

    private Booking sample(BookingStatus status) {
        return Booking.builder()
                .id(UUID.randomUUID())
                .studentId(UUID.randomUUID())
                .profId(UUID.randomUUID())
                .slotId(UUID.randomUUID())
                .subjectCode("MATH")
                .level(SchoolLevel.LYCEE)
                .modality(Modality.ONLINE)
                .durationMinutes((short) 60)
                .amountMinor(15000L)
                .platformFeeMinor(1500L)
                .totalAmountMinor(16500L)
                .currency("MAD")
                .status(status)
                .scheduledStart(Instant.now().plusSeconds(3600))
                .scheduledEnd(Instant.now().plusSeconds(7200))
                .declineReason("Out of office")
                .declinedAt(Instant.now())
                .build();
    }

    @Test
    void detailedView_preAccept_hidesDeclineReason() {
        // ANTI-BYPASS: before ACCEPTEE_PAR_PROF, internal decisions are not leaked.
        Booking b = sample(BookingStatus.DEMANDEE);
        BookingDetailedView v = masker.toDetailedView(b, Role.STUDENT);
        assertThat(v.declineReason()).isNull();
    }

    @Test
    void detailedView_afterAccept_revealsDeclineReason() {
        Booking b = sample(BookingStatus.ANNULEE);
        BookingDetailedView v = masker.toDetailedView(b, Role.STUDENT);
        assertThat(v.declineReason()).isEqualTo("Out of office");
    }

    @Test
    void detailedView_neverContainsPhoneOrEmail_byDtoSchema() {
        // The BookingDetailedView record has no phone/email field — verified at compile time.
        // This test documents the contract: even with a contrived booking, no PII fields
        // are exposed by the view.
        Booking b = sample(BookingStatus.CONFIRMEE);
        BookingDetailedView v = masker.toDetailedView(b, Role.ADMIN);
        // Reflectively assert no field name in the record contains "phone" or "email".
        for (var field : BookingDetailedView.class.getDeclaredFields()) {
            String n = field.getName().toLowerCase();
            assertThat(n).doesNotContain("phone");
            assertThat(n).doesNotContain("email");
        }
        assertThat(v).isNotNull();
    }

    @Test
    void listView_exposesOnlyOpaqueUuids() {
        Booking b = sample(BookingStatus.ACCEPTEE_PAR_PROF);
        var v = masker.toListView(b);
        assertThat(v.profId()).isEqualTo(b.getProfId());
        assertThat(v.studentId()).isEqualTo(b.getStudentId());
        assertThat(v.totalAmountMinor()).isEqualTo(16500L);
    }
}
