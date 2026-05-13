package ma.naja7i.calendar.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ConfirmSlotRequest(@NotNull UUID bookingId) {}
