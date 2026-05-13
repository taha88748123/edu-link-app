package ma.naja7i.pricing.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import ma.naja7i.shared.dto.Modality;
import ma.naja7i.shared.dto.SchoolLevel;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MarketSuggestionView(
        UUID id,
        UUID profId,
        String subjectCode,
        SchoolLevel level,
        Modality modality,
        Integer durationMinutes,
        long suggestedAmountMinor,
        String suggestedAmountMad,
        Instant computedAt,
        String source
) {
}
