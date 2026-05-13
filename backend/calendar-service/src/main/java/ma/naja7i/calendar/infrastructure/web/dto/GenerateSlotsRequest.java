package ma.naja7i.calendar.infrastructure.web.dto;

/**
 * Optional body for {@code POST /api/v1/calendar/slots:generate}. In v1 the materializer
 * always uses the service-level window ({@code naja7i.calendar.generation-window-days}),
 * so this is currently empty. Kept for forward-compatibility with v2 (per-request window).
 */
public record GenerateSlotsRequest() {}
