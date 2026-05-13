package ma.naja7i.calendar.infrastructure.web.dto;

import java.util.UUID;

public record GenerateSlotsResponse(UUID profId, int created, int skipped) {}
