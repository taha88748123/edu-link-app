package ma.naja7i.auth.infrastructure.web.dto;

import jakarta.validation.constraints.Size;

public record SuspendRequest(
        @Size(max = 500)
        String reason
) {}
