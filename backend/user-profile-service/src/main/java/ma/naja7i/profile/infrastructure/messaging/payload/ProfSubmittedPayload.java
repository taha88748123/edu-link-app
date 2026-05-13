package ma.naja7i.profile.infrastructure.messaging.payload;

import java.util.List;
import java.util.UUID;

public record ProfSubmittedPayload(
        UUID userId,
        List<String> subjects,
        int diplomasCount
) { }
