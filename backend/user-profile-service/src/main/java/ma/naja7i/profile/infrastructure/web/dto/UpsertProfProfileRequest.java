package ma.naja7i.profile.infrastructure.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ma.naja7i.shared.dto.Modality;
import ma.naja7i.shared.dto.SchoolLevel;

import java.util.List;

public record UpsertProfProfileRequest(
        @Min(0) @Max(70) Integer yearsExperience,
        String introVideoUrl,
        List<SubjectEntry> subjects,
        List<Modality> modalities,
        List<LocationEntry> locations
) {

    public record SubjectEntry(
            @NotBlank String code,
            @NotNull SchoolLevel level) {}

    public record LocationEntry(
            @NotBlank String city,
            String region) {}
}
