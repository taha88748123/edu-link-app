package ma.naja7i.calendar.infrastructure.messaging.payload;

import ma.naja7i.shared.dto.Modality;

import java.time.Instant;
import java.util.UUID;

/** Payload of {@code naja7i.calendar.slot.created.v1}. */
public record SlotCreatedPayload(
        UUID slotId,
        UUID profId,
        Instant startAt,
        Instant endAt,
        Modality modality
) {}
