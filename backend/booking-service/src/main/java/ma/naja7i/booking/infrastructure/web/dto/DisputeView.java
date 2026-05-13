package ma.naja7i.booking.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import ma.naja7i.booking.domain.DisputeStatus;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DisputeView(
        UUID id,
        UUID bookingId,
        UUID openedBy,
        String cause,
        String evidenceUrls,
        DisputeStatus status,
        String resolutionNotes,
        UUID resolvedBy,
        Instant resolvedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
