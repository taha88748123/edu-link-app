package ma.naja7i.calendar.infrastructure.web.dto;

/**
 * Optional body for {@code POST /api/v1/calendar/slots/{id}:block}.
 * The PROF may provide a free-text reason; in v1 it is not persisted (only logged).
 */
public record BlockSlotRequest(String reason) {}
