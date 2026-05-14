package ma.naja7i.chat.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Moderation metadata attached to a message. Stored embedded in the {@code messages}
 * document.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Moderation {

    private ModerationStatus status;

    /** Pattern names that fired, e.g. {@code PHONE_PATTERN}, {@code EMAIL_PATTERN}, {@code AI_BYPASS_INTENT}. */
    @Builder.Default
    private List<String> flags = new ArrayList<>();

    /** Sample of the masked tokens (already redacted, never the original PII). */
    @Builder.Default
    private List<String> maskedFields = new ArrayList<>();

    public static Moderation clean() {
        return Moderation.builder()
                .status(ModerationStatus.CLEAN)
                .flags(new ArrayList<>())
                .maskedFields(new ArrayList<>())
                .build();
    }
}
