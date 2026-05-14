package ma.naja7i.chat.application;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import ma.naja7i.chat.domain.ModerationStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Outcome of the synchronous regex moderation step. Carries:
 * <ul>
 *   <li>{@code sanitizedBody} — the body to persist & deliver to clients</li>
 *   <li>{@code originalBody} — the as-typed body (admin-only)</li>
 *   <li>{@code status / flags / maskedFields} — to populate {@link ma.naja7i.chat.domain.Moderation}</li>
 * </ul>
 */
@Data
@Builder
@AllArgsConstructor
public class ModeratorResult {

    private String sanitizedBody;
    private String originalBody;
    private ModerationStatus status;

    @Builder.Default
    private List<String> flags = new ArrayList<>();

    @Builder.Default
    private List<String> maskedFields = new ArrayList<>();

    public boolean wasModified() {
        return status == ModerationStatus.MASKED
            || status == ModerationStatus.FLAGGED
            || status == ModerationStatus.BLOCKED;
    }

    public static ModeratorResult clean(String body) {
        return ModeratorResult.builder()
                .sanitizedBody(body)
                .originalBody(body)
                .status(ModerationStatus.CLEAN)
                .build();
    }
}
