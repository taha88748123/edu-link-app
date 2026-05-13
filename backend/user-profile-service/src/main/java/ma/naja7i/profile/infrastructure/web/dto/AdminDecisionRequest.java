package ma.naja7i.profile.infrastructure.web.dto;

import jakarta.validation.constraints.Size;

public record AdminDecisionRequest(
        @Size(max = 2000) String notes,
        @Size(max = 2000) String reason
) { }
