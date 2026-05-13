package ma.naja7i.profile.infrastructure.web.dto;

import ma.naja7i.shared.dto.Modality;
import ma.naja7i.shared.dto.ProfStatus;
import ma.naja7i.shared.dto.SchoolLevel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Full view of a prof profile — used for the prof himself or for ADMIN.
 */
public record ProfPrivateView(
        UUID userId,
        ProfStatus status,
        Integer yearsExperience,
        String introVideoUrl,
        BigDecimal aiQcmScore,
        String adminNotes,
        Instant submittedAt,
        Instant validatedAt,
        UUID validatedBy,
        List<SubjectView> subjects,
        List<Modality> modalities,
        List<LocationView> locations,
        List<DiplomaView> diplomas,
        BigDecimal avgStars,
        long reviewCount
) {

    public record SubjectView(String code, SchoolLevel level) {}

    public record LocationView(String city, String region) {}
}
