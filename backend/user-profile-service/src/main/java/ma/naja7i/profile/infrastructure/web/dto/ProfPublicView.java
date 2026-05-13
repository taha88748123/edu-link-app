package ma.naja7i.profile.infrastructure.web.dto;

import ma.naja7i.shared.dto.Modality;
import ma.naja7i.shared.dto.SchoolLevel;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Public view of a validated prof. Contact info is masked unless caller is ADMIN.
 */
public record ProfPublicView(
        UUID userId,
        String displayName,
        String photoUrl,
        String bio,
        Integer yearsExperience,
        String introVideoUrl,
        List<SubjectView> subjects,
        List<Modality> modalities,
        List<LocationView> locations,
        BigDecimal avgStars,
        long reviewCount
) {

    public record SubjectView(String code, SchoolLevel level) {}

    public record LocationView(String city, String region) {}
}
