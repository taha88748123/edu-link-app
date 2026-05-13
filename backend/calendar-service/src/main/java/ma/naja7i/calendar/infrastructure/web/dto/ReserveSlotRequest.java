package ma.naja7i.calendar.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Optional payload of {@code POST /internal/calendar/slots/{id}:reserve}. */
public record ReserveSlotRequest(@NotNull UUID bookingId) {}
