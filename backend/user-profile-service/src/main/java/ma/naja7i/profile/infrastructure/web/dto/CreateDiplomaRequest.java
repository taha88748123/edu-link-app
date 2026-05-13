package ma.naja7i.profile.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateDiplomaRequest(
        @NotBlank String title,
        String institution,
        Integer year
) { }
