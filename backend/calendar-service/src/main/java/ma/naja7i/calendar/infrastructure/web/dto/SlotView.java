package ma.naja7i.calendar.infrastructure.web.dto;

import ma.naja7i.calendar.domain.Slot;
import ma.naja7i.calendar.domain.SlotStatus;
import ma.naja7i.shared.dto.Modality;

import java.time.Instant;
import java.util.UUID;

public record SlotView(
        UUID id,
        UUID profId,
        Instant startAt,
        Instant endAt,
        Modality modality,
        SlotStatus status,
        UUID bookingId,
        Instant reservedUntil
) {
    public static SlotView of(Slot s) {
        return new SlotView(
                s.getId(), s.getProfId(),
                s.getStartAt(), s.getEndAt(),
                s.getModality(), s.getStatus(),
                s.getBookingId(), s.getReservedUntil());
    }
}
