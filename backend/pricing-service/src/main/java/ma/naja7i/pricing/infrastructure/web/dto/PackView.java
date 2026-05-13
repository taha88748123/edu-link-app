package ma.naja7i.pricing.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import ma.naja7i.shared.dto.Modality;
import ma.naja7i.shared.dto.SchoolLevel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PackView(
        UUID id,
        UUID profId,
        String subjectCode,
        SchoolLevel level,
        Modality modality,
        Integer sessionsCount,
        BigDecimal discountPct,
        long totalAmountMinor,
        String totalAmountMad,
        String currency,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
