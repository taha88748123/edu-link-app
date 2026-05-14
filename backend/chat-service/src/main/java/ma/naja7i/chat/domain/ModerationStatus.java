package ma.naja7i.chat.domain;

/**
 * Outcome of the {@link ma.naja7i.chat.application.MessageModerator}.
 * <ul>
 *   <li>{@code CLEAN}    — no suspicious pattern, body is delivered as-is.</li>
 *   <li>{@code MASKED}   — phone / email / URL was found and replaced with {@code ***}.</li>
 *   <li>{@code FLAGGED}  — heuristic + async AI flagged this as a likely bypass attempt
 *                          (delivered, but the admin and the strike tracker get notified).</li>
 *   <li>{@code BLOCKED}  — reserved for future enforcement; v1 never produces this.</li>
 * </ul>
 */
public enum ModerationStatus {
    CLEAN, MASKED, FLAGGED, BLOCKED
}
